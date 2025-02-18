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


(defn vote-an-option
  [opiton-id user-id]
  (-> opiton-id
      get-vote-queue
      (.add user-id)))


(defn get-vote-count
  [option-id]
  (-> option-id 
      get-vote-queue
      .size))
