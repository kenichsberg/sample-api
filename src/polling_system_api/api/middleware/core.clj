(ns polling-system-api.api.middleware.core
  (:require [polling-system-api.api.auth.core :as auth]
            [ring.util.http-response :as http-response]))


(defn- user-valid? [req]
  (let [user-id (auth/get-user-id req)]
    (if (auth/user-id-exists? user-id)
     req
     (throw (ex-info "The povided API key is invalid"
                     {:type :reitit.ring/response
                      :response (http-response/unauthorized)})))))

(def existing-user 
  {:name ::existing-user
   :wrap (fn [handler]
           (fn [req]
             (-> req user-valid? handler)))}) 
