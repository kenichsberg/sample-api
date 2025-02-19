(ns polling-system-api.repository.option
  (:require [polling-system-api.repository.vote :as repo.vote]))



(defn create-options!
  [options]
  (->> options
       (map-indexed 
         (fn [idx option] 
           (let [option-id (str (random-uuid))]
             (repo.vote/new-vote! option-id)
             [option-id {:vote-count 0
                         :option option
                         :rank idx}])))
       (into {})))
