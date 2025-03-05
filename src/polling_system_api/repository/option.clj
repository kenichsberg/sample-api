(ns polling-system-api.repository.option
  (:require [polling-system-api.repository.vote :as repo.vote]))



(defn create-options!
  [poll-id options]
  (->> options
       (map-indexed 
         (fn [idx option] 
           (let [option-id (str (random-uuid))]
             (repo.vote/add-vote! poll-id option-id)
             [option-id {:vote-count 0
                         :option option
                         :rank idx}])))
       (into {})))
