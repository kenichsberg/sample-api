(ns polling-system-api.repository.poll
  (:require [polling-system-api.repository.option :as repo.option]
            [polling-system-api.storage.core :as storage]))


(defn- assoc-poll
  ([poll-id question options]
   (swap! (:polls storage/db)
          update
          poll-id 
          assoc
          :question question
          :options options))

  ([poll-id user-id question options]
   (swap! (:polls storage/db)
          assoc
          poll-id {:user-id user-id
                   :question question
                   :options options})))


(defn- dissoc-poll
  [poll-id]
  (swap! (:polls storage/db) dissoc poll-id))


(defn create-poll 
  [user-id {:keys [poll-id question options] :as _poll}]
  (let [options' (repo.option/create-options! options)]
    (assoc-poll poll-id user-id question options')
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
  (let [options' (repo.option/create-options! options)]
    (assoc-poll poll-id question options')
    {:poll-id poll-id
     :question question
     :options options'}))


(defn delete-poll 
  [poll-id]
  (dissoc-poll poll-id))
