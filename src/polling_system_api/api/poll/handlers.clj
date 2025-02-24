(ns polling-system-api.api.poll.handlers
  (:require [clojure.core.async :as a]
            [polling-system-api.api.auth.core :as auth]
            [polling-system-api.repository.poll :as repo.poll]
            [polling-system-api.globals.channels :as channels]
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
          (repo.vote/remove-vote option-id))
        (http-response/ok))

      :else
      (http-response/forbidden))))


(defn- do-get-poll-result
  [poll-info]
  (->> poll-info :options
       (mapv (fn [[option-id option-map]]
               (let [vote-count (repo.vote/get-vote-count option-id)]
                 [option-id (assoc option-map :vote-count vote-count)])))
       (into {})))


#_(defn get-poll-result
  [{{{:keys [poll-id]} :path} :parameters :as  _req}]
  (if-let [poll-info (repo.poll/read-poll-info poll-id)]
    (http-response/ok (assoc poll-info :options (do-get-poll-result poll-info)))
    (http-response/not-found (format "poll-id '%s' was not found" poll-id))))


(defn subscribe-change [poll-id wait-time-seconds]
  (let [sub-channel (a/chan 1) 
        _ (a/sub channels/sub-root poll-id sub-channel)
        timeout-ch (a/timeout (* wait-time-seconds 1000))
        ;; NOTE This blocks thread, but if we have virtual threads, it doesn't affect.
        [msg _] (a/alts!! [sub-channel timeout-ch])]
    (a/unsub channels/sub-root poll-id sub-channel)
    (a/close! sub-channel)
    msg))


(defn get-poll-result
  [{{{:keys [poll-id]} :path
     {:keys [wait-time-seconds]} :query} :parameters
    :as  _req}]
  (let [wait-time-seconds (cond
                            (nil? wait-time-seconds) 20
                            (< 30 wait-time-seconds) 30
                            :else wait-time-seconds)
        poll-info (repo.poll/read-poll-info poll-id)]

    (cond
      (nil? poll-info)
      (http-response/not-found (format "poll-id '%s' was not found" poll-id))

      (zero? wait-time-seconds)
      (http-response/ok (assoc poll-info :options (do-get-poll-result poll-info)))

      (nil? (subscribe-change poll-id wait-time-seconds))
      (http-response/no-content)

      :else
      (http-response/ok (assoc poll-info :options (do-get-poll-result poll-info))))))
