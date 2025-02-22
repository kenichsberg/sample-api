(ns polling-system-api.api.poll.handlers
  (:require [polling-system-api.api.auth.core :as auth]
            [polling-system-api.repository.poll :as repo.poll]
            [ring.util.http-response :as http-response]
            [polling-system-api.repository.vote :as repo.vote]))


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
        (http-response/ok))

      :else
      (http-response/forbidden))))


(defn get-poll-result
  [{{{:keys [poll-id]} :path} :parameters :as  _req}]
  (if-let [poll-info (repo.poll/read-poll-info poll-id)]
    (let [result (->> poll-info :options
                      (mapv (fn [[option-id option-map]]
                              (let [vote-count (repo.vote/get-vote-count option-id)]
                                [option-id (assoc option-map :vote-count vote-count)])))
                      (into {}))]
      (http-response/ok (assoc poll-info :options result)))
    (http-response/not-found (format "poll-id '%s' was not found" poll-id))))
