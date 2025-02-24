(ns polling-system-api.api.poll.core-test
  (:require [clojure.test :refer [deftest testing is use-fixtures]]
            [polling-system-api.core :as root]
            [polling-system-api.globals.storage :as storage]
            [polling-system-api.utils.test-utils :as utils]
            [ring.mock.request :as mock]))


(def app (root/app))


(defn fixture-each [f]
  (reset! (:polls storage/db) {})
  (reset! (:votes storage/db) {})
  (f))

(use-fixtures :each fixture-each)



(deftest happy-path
  (testing "poll is created"
    (let [resp (app (-> (mock/request :post "/api/poll")
                        (mock/header "Authorization" "Bearer 123user1")
                        (mock/json-body {:poll-id "foo"
                                         :question "What is this?"
                                         :options ["a dog" "a cat" "a hyena"]})))
          #_#__ (def _resp resp)

          {:keys [poll-id question options] :as body}
          (utils/parse-json (:body resp))]

      (def _body body)

      (is (= 200 (:status resp)))

      (is (= "foo" poll-id))
      (is (= "What is this?" question))
      (is (= #{{:vote-count 0
                :option "a dog"
                :rank 0}
               {:vote-count 0
                :option "a cat"
                :rank 1}
               {:vote-count 0
                :option "a hyena"
                :rank 2}}
             (-> options vals set)))))


  (testing "poll result can be viewed"
    (let [resp (app (-> (mock/request :get "/api/poll/foo?wait-time-seconds=0")
                        (mock/header "Authorization" "Bearer 123user1")))

          {:keys [poll-id question options] :as body}
          (utils/parse-json (:body resp))]

      (def _body-poll-result body)
      (def option1-id (some (fn [[k v]] 
                              (when (= 0 (:rank v))
                                (name k))) 
                            options))
      (is (= 200 (:status resp)))

      (is (= "foo" poll-id))
      (is (= "What is this?" question))
      (is (= #{{:vote-count 0
                :option "a dog"
                :rank 0}
               {:vote-count 0
                :option "a cat"
                :rank 1}
               {:vote-count 0
                :option "a hyena"
                :rank 2}}
             (-> options vals set)))))


  (testing "vote count has changed (long-poll) "
    (let [resp-result-fut (future (app (-> (mock/request :get "/api/poll/foo?wait-time-seconds=3")
                                           (mock/header "Authorization" "Bearer 123user1"))))
          ;; NOTE Fleaky without sleep (timing - vote can win)
          _ (Thread/sleep 10)
          resp-vote (app (-> (mock/request :post (format "/api/option/%s" option1-id))
                             (mock/header "Authorization" "Bearer 123user1")))
          _ (is (= 200 (:status resp-vote)))
          resp @resp-result-fut
          #_#__ (def _rrre resp)

          {:keys [poll-id question options] :as body}
          (utils/parse-json (:body resp))]

      (def _body-long-poll-result body)
      (is (= 200 (:status resp)))

      (is (= "foo" poll-id))
      (is (= "What is this?" question))
      (is (= #{{:vote-count 1
                :option "a dog"
                :rank 0}
               {:vote-count 0
                :option "a cat"
                :rank 1}
               {:vote-count 0
                :option "a hyena"
                :rank 2}}
             (-> options vals set)))))


  (testing "poll can be edited"
    (let [resp (app (-> (mock/request :put "/api/poll/foo")
                        (mock/header "Authorization" "Bearer 123user1")
                        (mock/json-body {:question "What is that?"
                                         :options ["a dog" "a cat" "a human"]})))
          #_#__ (def _resp-poll-edit resp)

          {:keys [poll-id question options] :as body}
          (utils/parse-json (:body resp))]

      (def _body-poll-edit body)
      (is (= 200 (:status resp)))

      (is (= "foo" poll-id))
      (is (= "What is that?" question))
      (is (= #{{:vote-count 0
                :option "a dog"
                :rank 0}
               {:vote-count 0
                :option "a cat"
                :rank 1}
               {:vote-count 0
                :option "a human"
                :rank 2}}
             (-> options vals set)))))



  (testing "poll can be deleted"
    (let [resp (app (-> (mock/request :delete "/api/poll/foo")
                        (mock/header "Authorization" "Bearer 123user1")))
          #_#__ (def _resp-poll-delete resp)
          body (utils/parse-json (:body resp))]

      (def _body-poll-delete body)
      (is (= 200 (:status resp)))
      (is (nil? body)))))



(deftest create-poll-unhappy-paths
  (testing "poll is created"
    (app (-> (mock/request :post "/api/poll")
             (mock/header "Authorization" "Bearer 123user1")
             (mock/json-body {:poll-id "foo"
                              :question "What is this?"
                              :options ["a dog" "a cat" "a hyena"]}))))


  (testing "poll-id conflict"
    (let [resp (app (-> (mock/request :post "/api/poll")
                        (mock/header "Authorization" "Bearer 123user1")
                        (mock/json-body {:poll-id "foo"
                                         :question "What is this?"
                                         :options ["a dog" "a cat" "a hyena"]})))
          #_#__ (def _resp resp)

          body (utils/parse-json (:body resp))]

      (def _body-cpup-id-conflict body)

      (is (= 422 (:status resp)))))


  (testing "validation -"
    (testing "lacks keys:"
      (testing "poll-id"
        (let [resp (app (-> (mock/request :post "/api/poll")
                            (mock/header "Authorization" "Bearer 123user1")
                            (mock/json-body {:question "What is this?"
                                             :options ["a dog" "a cat" "a hyena"]})))
              #_#__ (def _resp resp)

              body (utils/parse-json (:body resp))]

          (def _body-cpup-validation1 body)

          (is (= 400 (:status resp)))))

      (testing "question"
        (let [resp (app (-> (mock/request :post "/api/poll")
                            (mock/header "Authorization" "Bearer 123user1")
                            (mock/json-body {:poll-id "bar"
                                             :options ["a dog" "a cat" "a hyena"]})))
              #_#__ (def _resp resp)

              body (utils/parse-json (:body resp))]

          (def _body-cpup-validation2 body)

          (is (= 400 (:status resp)))))

      (testing "options"
        (let [resp (app (-> (mock/request :post "/api/poll")
                            (mock/header "Authorization" "Bearer 123user1")
                            (mock/json-body {:poll-id "bar"
                                             :question "What is this?"})))
              #_#__ (def _resp resp)

              body (utils/parse-json (:body resp))]

          (def _body-cpup-validation3 body)

          (is (= 400 (:status resp))))))
    )


  (testing "unauthorized user"
    (let [resp (app (-> (mock/request :post "/api/poll")
                        (mock/header "Authorization" "Bearer unknown-user")
                        (mock/json-body {:poll-id "bar"
                                         :question "What is this?"
                                         :options ["a dog" "a cat" "a hyena"]})))
          #_#__ (def _resp resp)

          body (utils/parse-json (:body resp))]

      (def _body-cpup-uu body)

      (is (= 401 (:status resp))))))



(deftest get-poll
  (testing "non existent id"
    (let [resp (app (-> (mock/request :get "/api/poll/non-exitent-id?wait-time-seconds=3")
                        (mock/header "Authorization" "Bearer 123user1")))
          #_#__ (def _resp resp)
          body (utils/parse-json (:body resp))]
      (def _body-wp body)

      (is (= 404 (:status resp)))))

  (testing "poll is created"
    (app (-> (mock/request :post "/api/poll")
             (mock/header "Authorization" "Bearer 123user1")
             (mock/json-body {:poll-id "foo"
                              :question "What is this?"
                              :options ["a dog" "a cat" "a hyena"]}))))
  
  (testing "no changes"
    (let [resp (app (-> (mock/request :get "/api/poll/foo?wait-time-seconds=1")
                        (mock/header "Authorization" "Bearer 123user1")))
          #_#__ (def _resp resp)
          body (utils/parse-json (:body resp))]
      (def _body-wp-nc body)

      (is (= 204 (:status resp))))))



(deftest edit-poll-unhappy-paths
  (testing "poll is created"
    (app (-> (mock/request :post "/api/poll")
             (mock/header "Authorization" "Bearer 123user1")
             (mock/json-body {:poll-id "foo"
                              :question "What is this?"
                              :options ["a dog" "a cat" "a hyena"]}))))

  (testing "non exitent id"
    (let [resp (app (-> (mock/request :put "/api/poll/non-exitent-id")
                        (mock/header "Authorization" "Bearer 123user1")
                        (mock/json-body {:question "What is this?"
                                         :options ["a dog" "a cat" "a hyena"]})))
          #_#__ (def _resp resp)

          body (utils/parse-json (:body resp))]

      (def _body-epup-nei body)

      (is (= 404 (:status resp)))))


  (testing "edit permission:"
    (testing "not permitted by other users"
      (let [resp (app (-> (mock/request :put "/api/poll/foo")
                          (mock/header "Authorization" "Bearer 123user2")
                          (mock/json-body {:question "What is this?"
                                           :options ["a dog" "a cat" "a hyena"]})))
            #_#__ (def _resp resp)

            body (utils/parse-json (:body resp))]

        (def _body-epup-np body)

        (is (= 403 (:status resp)))
        ))

    (testing "permitted by admin (successful)"
      (let [resp (app (-> (mock/request :put "/api/poll/foo")
                          (mock/header "Authorization" "Bearer 123adminkey")
                          (mock/json-body {:question "What is this?"
                                           :options ["a dog" "a cat" "a human"]})))
            #_#__ (def _resp resp)

            body (utils/parse-json (:body resp))]

        (def _body-epup-pa body)

        (is (= 200 (:status resp)))))


    (testing "validation -"
      (testing "lacks keys"
        (let [resp (app (-> (mock/request :put "/api/poll/foo")
                            (mock/header "Authorization" "Bearer 123user1")
                            (mock/json-body {:question "What is this?"})))
              #_#__ (def _resp resp)

              body (utils/parse-json (:body resp))]

          (def _body-epup-vl body)

          (is (= 400 (:status resp)))))

      (testing "poll-id can't be changed"
        (let [resp (app (-> (mock/request :put "/api/poll/foo")
                            (mock/header "Authorization" "Bearer 123user1")
                            (mock/json-body {:poll-id "try-to-change-id-explicitly"
                                             :question "What is this?"
                                             :options ["a dog" "a cat" "a human"]})))
              #_#__ (def _resp resp)

              body (utils/parse-json (:body resp))]

          (def _body-epup-pi body)

          (is (= 200 (:status resp)))
          (is (= "foo" (:poll-id body))))))))



(deftest delete-poll-unhappy-paths
  (testing "poll is created"
    (app (-> (mock/request :post "/api/poll")
             (mock/header "Authorization" "Bearer 123user1")
             (mock/json-body {:poll-id "foo"
                              :question "What is this?"
                              :options ["a dog" "a cat" "a hyena"]}))))


  (testing "delete permission:"
    (testing "not permitted by other users"
      (let [resp (app (-> (mock/request :delete "/api/poll/foo")
                          (mock/header "Authorization" "Bearer 123user2")))
            #_#__ (def _resp resp)

            body (utils/parse-json (:body resp))]

        (def _body-dpup-np body)

        (is (= 403 (:status resp)))))

    (testing "permitted by admin (successful)"
      (let [resp (app (-> (mock/request :delete "/api/poll/foo")
                          (mock/header "Authorization" "Bearer 123adminkey")))
            #_#__ (def _resp resp)

            body (utils/parse-json (:body resp))]

        (def _body-dpup-pa body)

        (is (= 200 (:status resp)))))))



(deftest method-not-allowed
  (testing ":get /api/poll -> 405"
    (let [resp (app (-> (mock/request :get "/api/poll")
                        (mock/header "Authorization" "Bearer 123user1")
                        ))
          _ (def _resp_405 resp)
          body (utils/parse-json (:body resp))]

      (def _body_405 body)

      (is (= 405 (:status resp)))
      (is (= "Method not allowed" body)))))



(deftest route-not-found
  (testing "404"
    (let [resp (app (-> (mock/request :get "/api/foo/")
                        (mock/header "Authorization" "Bearer 123user1")
                        ))
          _ (def _resp_404 resp)
          body (utils/parse-json (:body resp))]

      (def _body_404 body)

      (is (= 404 (:status resp)))
      (is (=  "Route not found" body)))))
