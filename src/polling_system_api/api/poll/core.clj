(ns polling-system-api.api.poll.core
  (:require [polling-system-api.api.poll.handlers :as h]))


;;
;; Poll endpoints
;;
;; POST   /api/poll            create a poll
;; GET    /api/poll/{poll-id}  view result of a poll
;; PUT    /api/poll/{poll-id}  update a poll
;; PATCH  /api/poll/{poll-id}  patch a poll
;; DELETE /api/poll/{poll-id}  delete a poll


(def api
  ["/poll" 
   [""
    {:post {:handler h/create-poll}}]
   ["/:poll-id" {:parameters {:path {:poll-id string?}}}
    [""
     {:get {:handler h/get-poll-result}
      :put {:middleware []
            :handler h/update-poll}
      :delete {:middleware []
               :handler h/delete-poll}}]]])
