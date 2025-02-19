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
     :post {:parameters {:body nil?}
            :responses {200 {:body nil?}
                        404 {:body string?}
                        422 {:body string?}}
            :handler h/vote}}]])
