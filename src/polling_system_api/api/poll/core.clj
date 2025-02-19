(ns polling-system-api.api.poll.core
  (:require [clojure.spec.alpha :as s]
            [polling-system-api.api.poll.handlers :as h]))


;;
;; Poll endpoints
;;
;; POST   /api/poll            create a poll
;; GET    /api/poll/{poll-id}  view result of a poll
;; PUT    /api/poll/{poll-id}  update a poll
;; PATCH  /api/poll/{poll-id}  patch a poll
;; DELETE /api/poll/{poll-id}  delete a poll


(s/def ::poll-id string?)
(s/def ::question string?)
(s/def :req/options (s/coll-of string?))
(s/def ::create-poll-request
  (s/keys :req-un [::poll-id ::question :req/options]))
(s/def ::edit-poll-request
  (s/keys :req-un [::question :req/options]))

(s/def ::vote-count number?)
(s/def ::option string?)
(s/def ::rank number?)
(s/def ::option-map
  (s/keys :req-un [::vote-count ::option ::rank]))
(s/def :resp/options (s/every-kv string? ::option-map))
(s/def ::poll-response
  (s/keys :req-un [::poll-id ::question :resp/options]))


(def api
  ["/poll" 
   [""
    {:post {:parameters {:body ::create-poll-request}
            :responses {200 {:body ::poll-response}
                        422 {:body string?}}
            :handler h/create-poll}}]
   ["/:poll-id"
    {:parameters {:path {:poll-id string?}}
     :get {:responses {200 {:body ::poll-response}
                       404 {:body string?}}
           :handler h/get-poll-result}
     :put {:parameters {:body ::edit-poll-request}
           :responses {200 {:body ::poll-response}
                       403 {:body nil?}
                       422 {:body string?}}
           :handler h/update-poll}
     :delete {:responses {200 {:body nil?}
                          403 {:body nil?}
                          404 {:body string?}}
              :handler h/delete-poll}}]])
