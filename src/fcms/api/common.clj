(ns fcms.api.common
  (:require [taoensso.timbre :refer (debug info warn error fatal spy)]
            [clojure.string :refer (join split)]
            [cheshire.core :as json]
            [liberator.representation :refer (ring-response)]))

(def UTF8 "utf-8")

(def malformed true)
(def good-json false)

(def missing-response
  (ring-response
    {:status 404
     :headers {"Content-Type" (format "text/plain;charset=%s" UTF8)}}))

(def missing-collection-response
  (ring-response
    {:status 404
     :body "Collection not found."
     :headers {"Content-Type" (format "text/plain;charset=%s" UTF8)}}))

(defn unprocessable-entity-response [reason]
  (ring-response
    {:status 422
      :body reason
      :headers {"Content-Type" (format "text/plain;charset=%s" UTF8)}}))

(defn only-accept [status media-type]
  (ring-response
    {:status status
     :body (format "Acceptable media type: %s\nAcceptable charset: %s" media-type UTF8)
     :headers {"Content-Type" (format "text/plain;charset=%s" UTF8)}}))

(defn location-response [path-parts body media-type]
  (ring-response
    {:body body
     :headers {"Location" (format "/%s" (join "/" path-parts))
               "Content-Type" (format "%s;charset=%s" media-type UTF8)}}))

(defn malformed-json?
  "Read in the body param from the request as a string, parse it into JSON, make sure all the
  keys are keywords, and then return it, mapped to :data as the 2nd value in a vector,
  with the first value indicating it's not malformed. Otherwise just indicate it's malformed."
  [ctx]
  (try
    (if-let [data (-> (get-in ctx [:request :body]) slurp (json/parse-string true))]
      ; handle case of a string which is valid JSON, but still malformed for us
      (do (when-not (map? data) (throw (Exception.)))
        [good-json {:data data}])
      malformed)
    (catch Exception e
      (debug "Request body not processable as JSON: " e)
      malformed)))

(defn known-content-type?
  [ctx content-type]
  (if-let [request-type (get-in ctx [:request :headers "content-type"])]
    (= (first (split content-type #";")) (first (split request-type #";")))
    true))

(defn check-input [check]
  (if (= check true) true [false {:reason check}]))