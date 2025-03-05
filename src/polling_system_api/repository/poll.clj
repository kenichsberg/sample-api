(ns polling-system-api.repository.poll
  (:require [polling-system-api.repository.option :as repo.option]
            [polling-system-api.globals.storage :as storage])
  (:import [java.util.concurrent ConcurrentLinkedQueue]))


(defn- assoc-poll!
  [poll-id user-id question options]
  (swap! (:polls storage/db)
         assoc
         poll-id {:user-id user-id
                  :question question
                  :options options}))

(defn- patch-poll!
  [poll-id question options]
  (swap! (:polls storage/db)
         update
         poll-id 
         assoc
         :question question
         :options options))


(defn- dissoc-poll!
  [poll-id]
  (swap! (:polls storage/db) dissoc poll-id))


(defn create-poll 
  [user-id {:keys [poll-id question options] :as _poll}]
  (let [options' (repo.option/create-options! poll-id options)]
    (assoc-poll! poll-id user-id question options')
    {:poll-id poll-id
     :question question
     :options options'}))


(defn read-poll
  [poll-id]
  (-> storage/db :polls deref (get poll-id)))


(defn read-poll-info 
  [poll-id]
  (some-> (read-poll poll-id)
          (assoc :poll-id poll-id)))


(defn update-poll 
  [poll-id {:keys [question options] :as _poll}]
  (let [options' (repo.option/create-options! poll-id options)]
    (patch-poll! poll-id question options')
    {:poll-id poll-id
     :question question
     :options options'}))


(defn delete-poll 
  [poll-id]
  (dissoc-poll! poll-id))


(defn new-user-viewed-queue! [poll-id]
  (swap! (:polls storage/db)
         assoc-in [poll-id :user-viewed]
         (ConcurrentLinkedQueue.)))

(defn get-user-viewed-queue ^ConcurrentLinkedQueue [poll-id]
  (get-in @(:polls storage/db) [poll-id :user-viewed]))

(defn add-user-viewed! [poll-id user-id]
  (when-let [queue (get-user-viewed-queue poll-id)]
    (.add queue user-id)))

(defn user-viewed? [poll-id user-id]
  (when-let [queue (get-user-viewed-queue poll-id)]
    (.contains queue user-id)))
