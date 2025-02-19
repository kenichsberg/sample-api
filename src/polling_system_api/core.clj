(ns polling-system-api.core
  (:require [polling-system-api.api.routes :refer [router swagger]]
            [reitit.ring :as ring]
            [ring.adapter.jetty :as jetty])
  (:import (org.eclipse.jetty.server Server)))

(defonce ^:private server-ref (atom nil))

(defn app [ctx]
  (ring/ring-handler
    (router ctx)
    #_(ring/routes swagger (ring/create-default-handler))))

(defn stop
  []
  (when @server-ref
    (.stop ^Server @server-ref)
    (reset! server-ref nil)
    (println "Server has stopped.")))

(defn start
  [ctx port]
  (stop)

  (let [server (jetty/run-jetty (app ctx) 
                                {:port (or port 0)
                                 :join? false})
        port-actual (-> server .getConnectors first .getLocalPort)]
    (reset! server-ref server)
    (println (format "Server has started on %s." port-actual))))

(defn -main
  [& [port]]
  (start {} (and port (Integer/parseInt port))))

(comment
  (start {} 8080)
  (stop)
  )

