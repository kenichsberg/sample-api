(ns polling-system-api.core
  (:gen-class)
  (:require [polling-system-api.api.routes :refer [router]]
            [reitit.ring :as ring]
            [ring.adapter.jetty :as jetty])
  (:import (java.util.concurrent Executors)
           (org.eclipse.jetty.server Server)
           (org.eclipse.jetty.util.thread QueuedThreadPool)))

(defonce ^:private server-ref (atom nil))


(defmacro compile-if
  [test then else]
  (if (try (eval test) (catch Throwable _ false))
    `(do ~then)
    `(do ~else)))

(defn have-virtual-threads? []
  (compile-if (Thread/ofVirtual) true false))


(defn app []
  (ring/ring-handler
    (router)
    (ring/routes 
      (ring/create-default-handler
        {:not-found (constantly {:status  404 
                                 :body "Route not found"})
         :method-not-allowed (constantly {:status  405
                                          :body "Method not allowed"})
         :not-acceptable (constantly {:status  406 
                                      :body "Unacceptable"})}))))


(defn stop []
  (when @server-ref
    (.stop ^Server @server-ref)
    (reset! server-ref nil)
    (println "Server has stopped.")))


(defn start [port]
  (stop)

  (let [vthreads? (have-virtual-threads?)
        thread-pool (when vthreads? (QueuedThreadPool.))
        _ (when vthreads?
            (.setVirtualThreadsExecutor thread-pool
                                        (Executors/newVirtualThreadPerTaskExecutor)))
        opts (cond-> {:port (or port 0)
                      :join? false}
               vthreads?
               (assoc :thread-pool thread-pool))
        server (jetty/run-jetty (app) opts)
        port-actual (-> server .getConnectors first .getLocalPort)]
    (reset! server-ref server)
    (println (format "Server has started on %s." port-actual))))


(defn -main
  ([]
   (start nil))

  ([port]
   (start (Integer/parseInt port))))


(comment
  (start 8080)
  (stop)
  )

