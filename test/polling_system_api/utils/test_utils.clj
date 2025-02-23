(ns polling-system-api.utils.test-utils
  (:require [jsonista.core :as j])
  (:import (java.io InputStream InputStreamReader BufferedReader)))



(defn try-parse-json [string]
  (try
    (j/read-value string j/keyword-keys-object-mapper)
    (catch com.fasterxml.jackson.core.JsonParseException e
      (println (.getMessage e))
      string)
    (catch com.fasterxml.jackson.databind.exc.MismatchedInputException e
      (println (.getMessage e))
      string)))


(defn is->json->map [is]
  (try
    (some-> is
            (InputStreamReader.)
            (BufferedReader.)
            .lines
            (.collect (java.util.stream.Collectors/joining "\n"))
            (try-parse-json))
    (catch Exception e
      (println ::is->json->map e))))


(defn parse-json
  [str-or-is]
  (cond
    (string? str-or-is) (try-parse-json str-or-is)
    (instance? InputStream str-or-is) (is->json->map str-or-is)
    :else str-or-is))
