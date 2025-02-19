(ns polling-system-api.api.option.handlers
  (:require [polling-system-api.api.auth.core :as auth]
            [polling-system-api.repository.vote :as repo.vote]
            [ring.util.http-response :as http-response]))


(defn vote 
  [{{{:keys [option-id]} :path} :parameters :as  req}]
  (let [queue (repo.vote/get-vote-queue option-id)
        user-id (auth/get-user-id req)]

    (cond
      (nil? queue)
      (http-response/not-found (format "option-id '%s' not found" option-id))

      (repo.vote/user-already-voted? queue user-id)
      (http-response/unprocessable-entity "The same user already voted")

      :else
      (do
        (repo.vote/vote-an-option queue user-id)
        (http-response/ok)))))
