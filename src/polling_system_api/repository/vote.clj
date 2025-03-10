(ns polling-system-api.repository.vote
  (:require [polling-system-api.globals.storage :as storage])
  (:import [java.util.concurrent ConcurrentLinkedQueue]))


(defn add-vote
  [poll-id option-id]
  (swap! (:votes storage/db)
         assoc
         option-id
         {:poll-id poll-id
          :queue (ConcurrentLinkedQueue.)}))


(defn remove-vote
  [option-id]
  (swap! (:votes storage/db) dissoc option-id))


(defn get-vote-map
  [option-id]
  (get @(:votes storage/db) option-id))


(defn user-already-voted?
  [^ConcurrentLinkedQueue queue user-id]
  (.contains queue user-id))


(defn vote-an-option
  [^ConcurrentLinkedQueue queue user-id]
  (.add queue user-id))


(defn get-vote-count
  [option-id]
  (-> option-id 
      get-vote-map
      :queue
      .size))
