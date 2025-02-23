(ns polling-system-api.globals.storage)

;;
;; data structure
;; polls
;; {<poll-id> {:user-id <user_id>
;;             :question <question>
;;             :options 
;;             {<option-id> {:vote-count <int>
;;                           :option <option>
;;                           :rank <int>}
;;              ...}}
;;  ...}
;;
;; votes
;; {<option-id> #{<user-id> ...}
;;  ...}
;;
(def users 
  {"123adminkey" {:name "admin"
                  :admin? true}
   "123user1" {:name "user1"}
   "123user2" {:name "user2"}})

(defonce db 
  {:users users
   :polls (atom {})
   :votes (atom {})})
