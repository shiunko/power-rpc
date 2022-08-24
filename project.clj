(defproject net.clojars.shiunko/power-rpc "0.0.1"
  :description "FIXME: write description"
  :url "http://www.zthc.net"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [slacker "0.17.0"]

                 [ring/ring-jetty-adapter "1.9.5"]

                 [org.clojure/tools.macro "0.1.5"]

                 [clj-http "3.12.3"]
                 ]
  :main ^:skip-aot power-rpc.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
