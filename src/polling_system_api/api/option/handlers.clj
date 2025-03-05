(ns polling-system-api.api.option.handlers
  (:require [clojure.core.async :as a]
            [polling-system-api.api.auth.core :as auth]
            [polling-system-api.globals.channels :as channels]
            [polling-system-api.repository.poll :as repo.poll]
            [polling-system-api.repository.vote :as repo.vote]
            [ring.util.http-response :as http-response]))


(defn vote 
  [{{{:keys [option-id]} :path} :parameters :as  req}]
  (let [{:keys [poll-id queue]} (repo.vote/get-vote-map option-id)
        user-id (auth/get-user-id req)]

    (cond
      (nil? queue)
      (http-response/not-found (format "option-id '%s' not found" option-id))

      (repo.vote/user-already-voted? queue user-id)
      (http-response/unprocessable-entity "The same user already voted")

      :else
      (do
        (repo.vote/vote-an-option! queue user-id)
        (repo.poll/new-user-viewed-queue! poll-id)
        (a/>!! channels/pub {:poll-id poll-id 
                             :message :poll-changed})
        (http-response/ok)))))
