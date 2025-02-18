(ns polling-system-api.repository.option
  (:require [polling-system-api.repository.vote :as repo.vote]))



(defn create-options!
  [options]
  (->> options
       (map-indexed 
         (fn [idx option] 
           (let [option-id (str (random-uuid))]
             (repo.vote/new-vote! option-id)
             [option-id {:option option
                         :order idx}])))
       (into {})))
