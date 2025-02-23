(ns polling-system-api.globals.channels
  (:require [clojure.core.async :as a]))


(def pub (a/chan 10000))

(def sub-root (a/pub pub :poll-id (fn [_] (a/sliding-buffer 1))))




(comment
  (defn subscribe []
    (let [wait-time-seconds' 5 #_(if (< 20 wait-time-seconds) 20 wait-time-seconds)
          sub-channel (a/chan 1)
          _ (a/sub sub-root "foo" sub-channel)
          timeout-ch (a/timeout (* wait-time-seconds' 1000))
          _ (prn "listening...")
          [v ch] (a/alts!! [sub-channel timeout-ch])
          _ (a/unsub sub-root "foo" sub-channel)
          _ (a/close! sub-channel)]
      (prn v)))

  (do 
    (future (subscribe))
    (future (subscribe))
    (a/>!! pub {:poll-id "foo"
                :message :poll-changed}))

  )
