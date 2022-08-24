(ns power-rpc.client)

(def host "127.0.0.1:12145")
(def protocol "clj")
(def rns "power-rpc.server.fn-export")
(def post-fn "(post-fn uri {:body args})" nil)

(defn set-post-fn
  [pfn]
  (alter-var-root #'post-fn (constantly pfn)))

(defn set-host
  [& [vhost vns vprotocol _]]
  (let [vhost (or vhost host)
        vns (or vns rns)
        vprotocol (or vprotocol protocol)]
    (alter-var-root #'host (constantly vhost))
    (alter-var-root #'rns (constantly vns))
    (alter-var-root #'protocol (constantly vprotocol))))

(def fn-remote
  (memoize
    (fn [rfn protocol]
      (str "http://" host "/" rns "/" rfn "." protocol))))

(defn call-remote
  [rfn & args]
  (if post-fn
    (let [response (post-fn (fn-remote rfn protocol) {:body (prn-str args)})]
      (read-string (get response :body "")))
    "(println \"post-fn not set!!!\")"
    ))

(defn eval-remote
  [rfn & args]
  (eval (apply call-remote rfn args)))