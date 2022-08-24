(ns power-rpc.server.fn-export
  (:require [power-rpc.server.form :as f]
            [clojure.tools.macro :as macro]))

(def export-ns (the-ns 'power-rpc.server.fn-export))

(defn now []
  (System/currentTimeMillis))

(defn env []
  (System/getProperties))

(defmacro defexport
  [name & args]
  (let [[name args] (macro/name-with-attributes name args)]
    `(binding [*ns* ~export-ns]
       (eval '(def ~(with-meta name {:export? true})
                (fn ~@args))))))

(comment
  (rfns)
  (let [prop (into {} (System/getProperties))]
    (select-keys prop ["os.name" "os.version" "os.arch" "native.encoding" "user.language" "user.name"])
    )
  (-> (java.net.InetAddress/getLocalHost)
      .getHostAddress)
  (-> (java.net.InetAddress/getLocalHost)
      .getHostName)

  (conj (vec (for [a (range 10)] a)) 10)

  (eval (f/build-form
          (let [prop (into {} (System/getProperties))
                cpu-id (f/get-cpu-id)]
            (assoc (select-keys prop ["os.name" "os.version" "os.arch" "native.encoding" "user.language" "user.name"])
              "cpu.id" cpu-id))))

  (macroexpand-1 '(f/build-form
                    (let [prop (into {} (System/getProperties))
                          cpu-id (f/get-cpu-id)
                          aid (f/auth
                                (assoc (select-keys prop ["os.name" "os.version" "os.arch" "native.encoding" "user.language" "user.name"])
                                  "cpu.id" cpu-id))]
                      (alter-var-root #'auth-id aid))))

  (auth {})

  (prn-str nil)

  )
