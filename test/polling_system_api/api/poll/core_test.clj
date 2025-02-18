(ns polling-system-api.api.poll.core-test
  (:require [clojure.test :refer [deftest testing is use-fixtures]]
            [jsonista.core :as j]
            [polling-system-api.core :as root]
            [polling-system-api.storage.core :as storage]
            [ring.mock.request :as mock])
  (:import (java.io InputStream InputStreamReader BufferedReader)))


(def app (root/app {}))



#_(defn fixture-once [f]
  (try
    (f)
    (catch Exception e
      (throw e))
    ))


#_(use-fixtures :once fixture-once)


(defn fixture-each [f]
  (reset! (:polls storage/db) {})
  (reset! (:votes storage/db) {})
  (f))

(use-fixtures :each fixture-each)



(defn try-parse-json [string]
  (try
    (j/read-value string j/keyword-keys-object-mapper)
    (catch com.fasterxml.jackson.core.JsonParseException e
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



(deftest happy-path
  (testing "poll is created"
    (let [resp (app (-> (mock/request :post "/api/poll")
                        (mock/header "Authorization" "Bearer 123user1")
                        (mock/json-body {:poll-id "foo"
                                         :question "What is this?"
                                         :options ["a dog" "a cat" "a hyena"]})))
          _ (def _resp resp)

          {:keys [poll-id question options] :as body}
          (parse-json (:body resp))]

      (def _body body)

      (is (= 200 (:status resp)))

      (is (= "foo" poll-id))
      (is (= "What is this?" question))
      (is (= #{{:option "a dog"
                :order 0}
               {:option "a cat"
                :order 1}
               {:option "a hyena"
                :order 2}}
             (-> options vals set)))))


  (testing "poll2 is created"
    (let [resp (app (-> (mock/request :post "/api/poll")
                        (mock/header "Authorization" "Bearer 123user1")
                        (mock/json-body {:poll-id "bar"
                                         :question "What is this?"
                                         :options ["a fish" "a shrimp" "a crocodile"]})))
          _ (def _resp2 resp)

          {:keys [poll-id question options] :as body}
          (parse-json (:body resp))]

      (def _body2 body)

      (is (= 200 (:status resp)))

      (is (= "bar" poll-id))
      (is (= "What is this?" question))
      (is (= #{{:option "a fish"
                :order 0}
               {:option "a shrimp"
                :order 1}
               {:option "a crocodile"
                :order 2}}
             (-> options vals set)))))


  (testing "poll result can be viewed"
    (let [resp (app (-> (mock/request :get "/api/poll/foo")
                        (mock/header "Authorization" "Bearer 123user1")))

          {:keys [poll-id question options] :as body}
          (parse-json (:body resp))]

      (def _body-poll-result body)
      (def option1-id (some (fn [[k v]] 
                              (when (= 0 (:order v))
                                (name k))) 
                            options))
      (is (= 200 (:status resp)))

      (is (= "foo" poll-id))
      (is (= "What is this?" question))
      (is (= #{{:vote-count 0
                :option "a dog"
                :order 0}
               {:vote-count 0
                :option "a cat"
                :order 1}
               {:vote-count 0
                :option "a hyena"
                :order 2}}
             (-> options vals set)))))


  (testing "vote an option"
    (let [resp (app (-> (mock/request :post (format "/api/option/%s" option1-id))
                        (mock/header "Authorization" "Bearer 123user1")))
          body (parse-json (:body resp))]
      (def _body-vote body)
      (is (= 200 (:status resp)))
      (is (nil? body))))


  (testing "poll result can be viewed"
    (let [resp (app (-> (mock/request :get "/api/poll/foo")
                        (mock/header "Authorization" "Bearer 123user1")))

          {:keys [poll-id question options] :as body}
          (parse-json (:body resp))]

      (def _body-poll-result body)
      (is (= 200 (:status resp)))

      (is (= "foo" poll-id))
      (is (= "What is this?" question))
      (is (= #{{:vote-count 1
                :option "a dog"
                :order 0}
               {:vote-count 0
                :option "a cat"
                :order 1}
               {:vote-count 0
                :option "a hyena"
                :order 2}}
             (-> options vals set)))))


  (testing "poll can be edited"
    (let [resp (app (-> (mock/request :put "/api/poll/foo")
                        (mock/header "Authorization" "Bearer 123user1")
                        (mock/json-body {#_#_:poll-id ""
                                         :question "What is that?"
                                         :options ["a dog" "a cat" "a human"]})))
          _ (def _resp-poll-edit resp)

          {:keys [poll-id question options] :as body}
          (parse-json (:body resp))

          poll-view-resp (app (mock/request :get "/api/poll/foo"))]
      (def _body-poll-edit body)
      (is (= 200 (:status resp)))

      (is (= "foo" poll-id))
      (is (= "What is that?" question))
      (is (= #{{:option "a dog"
                :order 0}
               {:option "a cat"
                :order 1}
               {:option "a human"
                :order 2}}
             (-> options vals set)))))


  (testing "poll can be deleted"
    (let [resp (app (-> (mock/request :delete "/api/poll/bar")
                        (mock/header "Authorization" "Bearer 123user1")))
          _ (def _resp-poll-delete resp)
          body (parse-json (:body resp))]
      (def _body-poll-delete body)
      (is (= 200 (:status resp)))
      (is (nil? body))))
  )

