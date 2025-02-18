(ns polling-system-api.api.option.handlers
  (:require [polling-system-api.api.auth.core :as auth]
            [polling-system-api.repository.vote :as repo.vote]
            [ring.util.http-response :as http-response]))


(defn vote 
  [{{{:keys [option-id]} :path} :parameters :as  req}]
  (def _r req)
  (repo.vote/vote-an-option option-id (auth/get-user-id req))
  (http-response/ok))
