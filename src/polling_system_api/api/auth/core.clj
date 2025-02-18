(ns polling-system-api.api.auth.core
  (:require [clojure.string :as str]
            [polling-system-api.storage.core :as storage]
            [ring.util.http-response :as http-response]))


(defn- parse-apikey [authorization-header-value]
  (try 
    (str/replace authorization-header-value #"^Bearer " "")

    (catch Exception _
      (throw (ex-info "Invalid header value"
                      {:type :reitit.ring/response
                       :response (http-response/bad-request {:message "Invalid header value"
                                                             :field "Authorization"
                                                             :value authorization-header-value})})))))


(defn get-user-id [{{:strs [authorization]} :headers :as _req}]
  (when authorization (parse-apikey authorization)))


(defn user-id-exists? [user-id]
  (when user-id (get-in storage/db [:users user-id])))


(defn get-user [req]
  (let [user-id (get-user-id req)]
    (some-> (get-in storage/db [:users user-id])
            (assoc :id user-id))))
