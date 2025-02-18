(ns polling-system-api.api.option.core
  (:require [polling-system-api.api.option.handlers :as h]))


;;
;; Option endpoints
;;
;; POST   /api/option/{option-id}   vote an option


(def api
  ["/option"
   ["/:option-id"
    {:parameters {:path {:option-id string?}}
     :post {:handler h/vote}}]])
