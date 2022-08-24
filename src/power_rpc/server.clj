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
                                               :funcs           (apply merge (map parse-funcs fn-coll))
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