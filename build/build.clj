(ns build
  (:require [clojure.tools.build.api :as b]))

(def app       "polling-system-api")
(def version   (format "0.0.%s" (b/git-count-revs nil)))
(def class-dir "target/classes")
(def basis     (b/create-basis {:project "deps.edn"}))
(def uber-file "target/polling_system_api.jar")

(defn clean [_]
  (b/delete {:path "target"}))


(defn uber [_]
  (clean nil)
  (b/copy-dir {:src-dirs ["src"]
               :target-dir class-dir})

  (b/compile-clj {:basis basis
                  :ns-compile ['polling-system-api.core] 
                  :class-dir class-dir})

  (b/uber {:class-dir class-dir
           :uber-file uber-file
           :basis     basis
           :main      'polling-system-api.core})

  :done)

(comment
  (clean nil)
  (uber nil)
  )

