(ns polling-system-api.api.option.core-test
  (:require [clojure.test :refer [deftest testing is use-fixtures]]
            [polling-system-api.core :as root]
            [polling-system-api.storage.core :as storage]
            [polling-system-api.utils.test-utils :as utils]
            [ring.mock.request :as mock]))


(def app (root/app))


(defn fixture-each [f]
  (reset! (:polls storage/db) {})
  (reset! (:votes storage/db) {})
  (f))

(use-fixtures :each fixture-each)



(deftest vote-api
  (testing "poll is created"
    (let [resp (app (-> (mock/request :post "/api/poll")
                        (mock/header "Authorization" "Bearer 123user1")
                        (mock/json-body {:poll-id "foo"
                                         :question "What is this?"
                                         :options ["a dog" "a cat" "a hyena"]})))

          {:keys [options] :as _body}
          (utils/parse-json (:body resp))]

      (def option1-id (some (fn [[k v]] 
                              (when (= 0 (:rank v))
                                (name k))) 
                            options))))


  (testing "1 vote per 1 user"
    (let [resp1 (app (-> (mock/request :post (format "/api/option/%s" option1-id))
                         (mock/header "Authorization" "Bearer 123user1")))
          _ (def _resp1 resp1)
          body1 (utils/parse-json (:body resp1))
          _ (def _body-vote1 body1)
          _ (is (= 200 (:status resp1)))
          
          ;; the same user can't vote twice
          resp2 (app (-> (mock/request :post (format "/api/option/%s" option1-id))
                         (mock/header "Authorization" "Bearer 123user1")))
          body2 (utils/parse-json (:body resp1))
          _ (def _body-vote2 body2)
          _ (is (= 422 (:status resp2)))

          ;; other users can vote (the first time)
          resp3 (app (-> (mock/request :post (format "/api/option/%s" option1-id))
                         (mock/header "Authorization" "Bearer 123user2")))
          body3 (utils/parse-json (:body resp1))
          _ (def _body-vote3 body3)
          _ (is (= 200 (:status resp3)))
          
          ;; poll result
          resp (app (-> (mock/request :get "/api/poll/foo")
                        (mock/header "Authorization" "Bearer 123user1")))
          {:keys [options] :as body4} (utils/parse-json (:body resp))]

      (def _body-poll-result body4)

      (is (= 200 (:status resp)))
      (is (= #{{:vote-count 2
                :option "a dog"
                :rank 0}
               {:vote-count 0
                :option "a cat"
                :rank 1}
               {:vote-count 0
                :option "a hyena"
                :rank 2}}
             (-> options vals set)))))

  (testing "option-id not found"
    (let [resp (app (-> (mock/request :post "/api/option/unexistent-option-id")
                        (mock/header "Authorization" "Bearer 123user1")))
          _ (def _resp resp)
          body (utils/parse-json (:body resp))
          _ (def _body-not-found body)
          _ (is (= 404 (:status resp)))]))
  )
