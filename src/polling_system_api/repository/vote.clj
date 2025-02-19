(ns polling-system-api.repository.vote
  (:require [polling-system-api.storage.core :as storage])
  (:import [java.util.concurrent ConcurrentLinkedQueue]))


(defn new-vote!
  [option-id]
  (swap! (:votes storage/db)
         assoc
         option-id
         (ConcurrentLinkedQueue.)))


(defn get-vote-queue ^ConcurrentLinkedQueue
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
      get-vote-queue
      .size))
