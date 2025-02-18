(ns polling-system-api.api.routes
  (:require [muuntaja.core]
            [polling-system-api.api.middleware.core :as mw]
            [polling-system-api.api.poll.core :as poll]
            [polling-system-api.api.option.core :as option]
            [reitit.coercion.spec]
            [reitit.dev.pretty :as rp]
            [reitit.openapi :as openapi]
            [reitit.ring :as ring]
            [reitit.ring.coercion :as coercion]
            [reitit.ring.middleware.exception :as exception]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.ring.middleware.parameters :as parameters]
            [reitit.ring.spec :as rrs]
            [reitit.swagger :as swagger]
            [reitit.swagger-ui :as swagger-ui]))


(defn router [{{meta :meta} :cfg
               #_#_:as          ctx}]
  (let [info {:title   (:product meta)
              :version (:version meta)}]
    (ring/router
      ["/api"
       poll/api
       option/api]

      {:validate  rrs/validate
       :exception rp/exception
       :data      {:coercion   reitit.coercion.spec/coercion
                   :muuntaja   muuntaja.core/instance
                   :middleware [parameters/parameters-middleware
                                muuntaja/format-negotiate-middleware
                                muuntaja/format-response-middleware
                                exception/exception-middleware
                                mw/existing-user
                                muuntaja/format-request-middleware
                                coercion/coerce-response-middleware
                                coercion/coerce-request-middleware]}})))

(def swagger
  (swagger-ui/create-swagger-ui-handler
    {:path "/"
     ;; See https://github.com/swagger-api/swagger-ui/blob/master/docs/usage/configuration.md
     :config {:validatorUrl     nil
              :urls             [{:name "swagger" :url "swagger.json"}
                                 {:name "openapi" :url "openapi.json"}]
              :urls.primaryName "openapi"}}))

