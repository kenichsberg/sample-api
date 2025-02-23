(ns polling-system-api.api.routes
  (:gen-class)
  (:require [muuntaja.core]
            [polling-system-api.api.middleware.core :as mw]
            [polling-system-api.api.poll.core :as poll]
            [polling-system-api.api.option.core :as option]
            [reitit.coercion.spec]
            [reitit.dev.pretty :as rp]
            [reitit.ring :as ring]
            [reitit.ring.coercion :as coercion]
            [reitit.ring.middleware.exception :as exception]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.ring.middleware.parameters :as parameters]
            [reitit.ring.spec :as rrs]))


(defn router []
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
                              coercion/coerce-request-middleware]}}))
