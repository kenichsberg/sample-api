(ns polling-system-api.api.poll.handlers
  (:require [clojure.core.async :as a]
            [polling-system-api.api.auth.core :as auth]
            [polling-system-api.globals.channels :as channels]
            [polling-system-api.repository.poll :as repo.poll]
            [polling-system-api.repository.vote :as repo.vote]
            [ring.util.http-response :as http-response]))


(defn create-poll 
  [{{:keys [poll-id] :as poll-request} :body-params :as req}]
  (if (repo.poll/read-poll poll-id)
    (http-response/unprocessable-entity (format "poll-id '%s' already exists" poll-id))
    (let [user-id (auth/get-user-id req)]
      (http-response/ok (repo.poll/create-poll user-id poll-request)))))


(defn user-permitted? [req poll]
  (let [requesting-user (auth/get-user req)
        poll-user-id (:user-id poll)]
    (or
      (:admin? requesting-user)
      (= (:id requesting-user) poll-user-id))))


(defn update-poll 
  [{{{:keys [poll-id]} :path} :parameters
    poll-request :body-params
    :as req}]
  (let [poll (repo.poll/read-poll poll-id)]
    (cond
      (nil? poll) 
      (http-response/not-found (format "poll-id '%s' was not found" poll-id))

      (user-permitted? req poll)
      (http-response/ok (repo.poll/update-poll poll-id poll-request))

      :else
      (http-response/forbidden))))


(defn delete-poll
  [{{{:keys [poll-id]} :path} :parameters :as req}]
  (let [poll (repo.poll/read-poll poll-id)]
    (cond
      (nil? poll)
      (http-response/not-found (format "poll-id '%s' was not found" poll-id))

      (user-permitted? req poll)
      (do
        (repo.poll/delete-poll poll-id)
        (doseq [[option-id _] (:options poll)]
          (repo.vote/remove-vote! option-id))
        (http-response/ok))

      :else
      (http-response/forbidden))))


(defn- get-vote-counts
  [poll-info]
  (->> poll-info :options
       (mapv (fn [[option-id option-map]]
               (let [vote-count (repo.vote/get-vote-count option-id)]
                 [option-id (assoc option-map :vote-count vote-count)])))
       (into {})))


(defn- do-get-poll-result
  [poll-info user-id]
  (repo.poll/add-user-viewed! (:poll-id poll-info) user-id)
  (assoc poll-info :options (get-vote-counts poll-info)))


(defn- poll-changed? [poll-id user-id]
  (false? (repo.poll/user-viewed? poll-id user-id)))


(defn- wait-poll-change!! [poll-id wait-time-seconds]
  (let [timeout-ch (a/timeout (* wait-time-seconds 1000))
        sub-channel (a/chan 1)]
    (try
      (a/sub channels/sub-root poll-id sub-channel)
      (try
        ;; NOTE This blocks a thread, but if we have virtual threads, it doesn't affect to throughputs.
        (let [[msg _] (a/alts!! [sub-channel timeout-ch])]
          msg)
        (finally 
          (a/unsub channels/sub-root poll-id sub-channel)))
      (finally
        (a/close! sub-channel)))))


(defn get-poll-result
  [{{{:keys [poll-id]} :path
     {:keys [wait-time-seconds]} :query} :parameters
    :as  req}]
  (let [wait-time-seconds (cond
                            (nil? wait-time-seconds) 20
                            (< 30 wait-time-seconds) 30
                            :else wait-time-seconds)
        poll-info (repo.poll/read-poll-info poll-id)
        user-id (auth/get-user-id req)]

    (cond
      (nil? poll-info)
      (http-response/not-found (format "poll-id '%s' was not found" poll-id))

      (zero? wait-time-seconds)
      (http-response/ok (do-get-poll-result poll-info user-id))
      
      (poll-changed? poll-id user-id)
      (http-response/ok (do-get-poll-result poll-info user-id))

      (nil? (wait-poll-change!! poll-id wait-time-seconds))
      (if (poll-changed? poll-id user-id)
        (http-response/ok (do-get-poll-result poll-info user-id))
        (http-response/no-content))

      :else
      (http-response/ok (do-get-poll-result poll-info user-id)))))
