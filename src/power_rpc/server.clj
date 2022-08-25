(ns power-rpc.server
  (:require
    [slacker.server :as server]
    [slacker.serialization :refer [serialize deserialize]]
    [ring.adapter.jetty :as jetty]
    [power-rpc.server.fn-export]
    )
  (:import (java.nio.charset Charset))
  )

(def server-future nil)

(def warp-exception (fn [handler]
                      (fn [request respond raise]
                        (try
                          (handler request respond raise)
                          (catch Exception e
                            (-> e .printStackTrace)
                            (respond {:status 500
                                      :body   ""}))))))

(defn start
  [port]
  (let [f (future
            (jetty/run-jetty
              (warp-exception (server/slacker-ring-app
                                (the-ns 'power-rpc.server.fn-export)))
              {:port   (int port)
               :async? true})
            :dead)]
    (alter-var-root #'server-future (constantly f)) f))

(defn handler
  []
  (warp-exception (server/slacker-ring-app
                    (the-ns 'power-rpc.server.fn-export)))
  )

(defn close
  []
  (or (when server-future
        (let [result (future-cancel server-future)]
          (alter-var-root #'server-future (constantly nil))
          result))
      false))

(defmacro add-serializer
  [protocol serialize-fn]
  `(defmethod serialize ~protocol
     [_# data#]
     (~serialize-fn data#)))

(defmacro add-deserializer
  [protocol deserialize-fn]
  `(defmethod deserialize ~protocol
     [_# buffer#]
     (~deserialize-fn buffer#)))

(comment

  (macroexpand-1 '(add-deserializer :zthc
                                    (fn [buffer]
                                      (let [s (.toString buffer (Charset/forName "UTF-8"))]
                                        (clojure.edn/read-string s)))))

  )

(in-ns 'slacker.server)

(defn error-packet
  [& args]
  {:code         :exception
   :content-type "plain/text"
   :result       (serialize :clj "error")}
  )

(defn- map-response-fields [req]
  (protocol/of (:protocol-version req)
               [(:tid req) [:type-response
                            (mapv req [:content-type :code :result :raw-extensions])]]))

(defn slacker-ring-app
  "Wrap slacker as a ring **async** app that can be deployed to any ring adaptors.
  You can also configure interceptors just like `start-slacker-server`"
  [fn-coll & {:keys [interceptors]
              :or   {interceptors interceptor/default-interceptors}}]
  (let [fn-coll (if (vector? fn-coll) fn-coll [fn-coll])
        server-pipeline (build-server-pipeline interceptors nil)]
    (fn [req resp-callback _]
      (let [client-info (http-client-info req)
            curried-handler (fn [req]
                              (handle-request {:server-pipeline server-pipeline
                                               :get-funcs       (fn [] (apply merge (map parse-funcs fn-coll)))
                                               :send-response   (fn [_ resp]
                                                                  (-> resp
                                                                      map-response-fields
                                                                      slacker-resp->ring-resp
                                                                      resp-callback))}
                                              req
                                              client-info))]
        (-> req
            ring-req->slacker-req
            curried-handler
            )))))

(defmethod -handle-request :type-request [{:keys [server-pipeline
                                                  inspect-handler
                                                  running-threads
                                                  executors
                                                  funcs
                                                  get-funcs
                                                  send-response]
                                           :or   {send-response link-write-response}}
                                          req client-info]
  (let [req-map (assoc (map-req-fields req) :client client-info)
        req-map (look-up-function req-map (or funcs (get-funcs)))]
    (if (nil? (:code req-map))
      (let [thread-pool (get executors (:ns-name req-map)
                             (:default executors))]
        (try
          (with-executor ^ThreadPoolExecutor thread-pool
                         ;; when thread-pool is nil the body will run on default
                         ;; thread. We keep it return null so we will deal with the
                         ;; result sending.
                         ;; async run on dedicated or global thread pool
                         ;; http call always runs on default thread
                         (try
                           (let [resp (server-pipeline req-map)]
                             (if-let [resp-future (:future resp)]
                               (d/chain resp-future (partial send-response client-info))
                               (send-response client-info resp)))
                           (finally
                             (release-buffer! req-map))))
          ;; async run, return nil so sync wait won't send
          (catch RejectedExecutionException _
            (log/warn "Server thread pool is full for" (:ns-name req-map))
            (release-buffer! req-map)
            (send-response (:channel client-info)
                           (error-packet (:version req-map) (:tid req-map) :thread-pool-full)))))
      ;; return error
      (try
        (send-response (:channel client-info)
                       (error-packet (:version req-map) (:tid req-map) :not-found))
        (finally
          (release-buffer! req-map))))))