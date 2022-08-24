(ns power-rpc.core
  (:gen-class)
  (:require [power-rpc.client :as client]
            [power-rpc.server :as server]
            [power-rpc.server.fn-export :as fe]
            [power-rpc.server.form :as f]
            ))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))

(comment
  (server/start 15151)

  (client/set-host "127.0.0.1:15151")

  (client/call-remote 'now)


  (require 'clj-http.client)
  (client/set-post-fn clj-http.client/post)

  (macroexpand-1 '(fe/defexport
                    info
                    [& args]
                    (f/build-form
                      (let [prop (into {} (System/getProperties))
                            cpu-id (f/get-cpu-id)]
                        (assoc (select-keys prop ["os.name" "os.version" "os.arch" "native.encoding" "user.language" "user.name"])
                          "cpu.id" cpu-id)))))

  (fe/defexport
    info
    [& args]
    (f/build-form
      (let [prop (into {} (System/getProperties))
            cpu-id (f/get-cpu-id)]
        (assoc (select-keys prop ["os.name" "os.version" "os.arch" "native.encoding" "user.language" "user.name"])
          "cpu.id" cpu-id))))

  (fe/info)

  (client/eval-remote 'info)

  (client/call-remote 'not-found)

  (load-string "")
  )
