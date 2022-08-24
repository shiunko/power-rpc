(ns power-rpc.server.form
  ^{:author "MoonLing"
    :doc "build client eval-able form in server"}
  (:require [clojure.tools.macro :as macro]))

(def form-ns (the-ns 'power-rpc.server.form))
(defn- ns-of
  [var]
  (get (meta var) :ns nil))
(defn- internal?
  [sym]
  (some? (get (meta (resolve sym)) :form-type)))
(defn- value-of
  [sym]
  (var-get (resolve sym)))
(defn- form?
  [sym]
  (or (instance? clojure.lang.PersistentList sym)
      (instance? clojure.lang.Cons sym))
  )

(def ^:private fn-sym?
  (memoize (fn [x]
             (some #(= % x) '[fn clojure.core/fn]))))

(defmacro build-form
  [& bodies]
  (let [bodies (for [body bodies]
                 (clojure.walk/postwalk
                   (fn [x]
                     (if (symbol? x)
                       (if (internal? x)
                         (var-get (resolve x))
                         x)
                       (if (and (form? x)
                                (= 1 (count x))
                                (form? (first x))
                                (nil? (fn-sym? (first (first x)))))
                         (first x)
                         x)
                       ))
                   body))
        bodies (vec bodies)]
    `(quote (do ~@bodies))))

(defmacro def-form
  [name & bodies]
  (let [[name bodies] (macro/name-with-attributes name bodies)]
    `(binding [*ns* ~form-ns]
      (eval '(def ~(with-meta name {:form-type :form}) '(do ~@bodies))))
    ))

(defmacro def-fn
  [name & bodies]
  (let [[name bodies] (macro/name-with-attributes name bodies)]
    `(binding [*ns* ~form-ns]
       (eval '(def ~(with-meta name {:form-type :fn}) '(fn ~@bodies))))
    ))

(def-form get-cpu-id
          (let [process (-> (Runtime/getRuntime)
                            (.exec (into-array String ["wmic" "cpu" "get" "ProcessorId"])))
                _ (-> process .getOutputStream .close)
                sc (java.util.Scanner. (-> process .getInputStream))]
            (.next sc)
            (.next sc)))

(def-fn get-server-now
        []
        (power-rpc.client/call-remote 'now))

(comment

  (meta (resolve 'get-cpu-id))

  (macroexpand-1 '(build-form
                    (let [cpu-id (get-cpu-id)]
                      (println cpu-id)
                      )
                    ))

  (type (second get-cpu-id))

  (build-form
    (let [cpu-id (get-cpu-id)]
      (println cpu-id)
      )
    )
  (eval (build-form
          (let [cpu-id (get-cpu-id)]
            cpu-id
            )
          ))

  (eval get-cpu-id)

  (let [cpu-id (get-cpu-id)]
    (println cpu-id))

  (clojure.walk/postwalk (fn [x] x) '(prn 123))

  (fn-sym? 'fn)

  (type (var-get #'net.zthc.project.remote-form/get-cpu-id))
  (ns-of #'net.zthc.project.remote-form/get-cpu-id)
  (namespace 'net.zthc.project.remote-form/get-cpu-id)
  )