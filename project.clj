(defproject net.clojars.shiunko/power-rpc "0.0.2"
  :description "FIXME: write description"
  :url "http://www.zthc.net"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [slacker "0.17.0"]

                 [ring/ring-jetty-adapter "1.9.5"]

                 [org.clojure/tools.macro "0.1.5"]

                 [clj-http "3.12.3"]
                 ]
  :main ^:skip-aot power-rpc.core
  :target-path "target/%s"
  :clr {:cmd-templates {:clj-exe   [[?PATH "mono"] ["E:\\CODE\\clojure-clr\\Clojure\\bin" %1]]
                        :clj-dep   [[?PATH "mono"] ["target/clr/clj/Debug 4.0" %1]]
                        :nuget-ver [[?PATH "mono"] [*PATH "nuget.exe"] "install" %1 "-Version" %2]
                        :nuget-any [[?PATH "mono"] [*PATH "nuget.exe"] "install" %1]}
        :deps-cmds     [
                        ;[:nuget-any "Shared"]
                        ;[:nuget-any "Server.Library"]
                        ;["copy" "/Y" "H:\\mir2dot\\zthc-clr\\assembly\\*" "."]
                        ]
        :main-cmd      [:clj-exe "Clojure.Main461.exe"]
        :compile-cmd   [:clj-exe "Clojure.Compile.exe"]
        ;:assembly-paths ["E:\\CODE\\zthc-clr\\assembly"]
        }
  :profiles {:uberjar {:aot      :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}
             :dev     {:plugins [[lein-clr "0.2.2"]]}
             })
