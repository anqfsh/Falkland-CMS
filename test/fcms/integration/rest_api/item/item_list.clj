(ns fcms.integration.rest-api.item.item-list
  "Integration tests for listing items with the REST API."
  (:require [midje.sweet :refer :all]
            [clj-time.format :refer (parse)]
            [fcms.lib.resources :refer :all]
            [fcms.lib.body :refer (verify-item-links)]
            [fcms.lib.rest-api-mock :refer :all]
            [fcms.resources.collection :as collection]
            [fcms.resources.item :as item]))

;; The system should return a summary list of the items stored in a collection and handle the following scenarios:
;;
;; GET
;; all good, empty collection
;; all good, 1 item
;; all good, many items
;; all good, force pagination
;; all good, additional page
;; no accept
;; wrong accept
;; no accept charset
;; wrong accept charset
;; collection doesn't exist
;; using an invalid page

;; ----- Utilities -----

(defn- items-from-body [body]
  (-> body
      :collection
      :items))

(defn- create-collection-one-items []
  (item/create-item one i {:slug i :description ascii-description}))

(defn- create-collection-many-items []
  (item/create-item many i {:slug i :description ascii-description})
  (item/create-item many unicode-name {:slug "uni-i" :description unicode-description})
  (item/create-item many "i 2" {:slug "i-2" :description (str ascii-description " 2")})
  (item/create-item many "i 3" {:slug "i-3" :description (str ascii-description " 3")})
  (item/create-item many "i 4" {:slug "i-4" :description (str ascii-description " 4")}))

(defn- setup []
  (reset-collection e)
  (reset-collection one)
  (create-collection-one-items)  
  (reset-collection many)
  (create-collection-many-items))

(defn- teardown []
  (collection/delete-collection e)
  (collection/delete-collection one)
  (collection/delete-collection many))

;; ----- Tests -----

(with-state-changes [(before :facts (setup))
                     (after :facts (teardown))]

  (facts "about using the REST API to list items"

    ;; all good, empty collection - 200 OK
    ;; curl -i --header "Accept: application/vnd.collection+vnd.fcms.item+json;version=1" --header "Accept-Charset: utf-8" -X GET http://localhost:3000/empty/
    (fact "from an empty collection"
      (let [response (api-request :get "/e/" {:headers {:Accept (mime-type :item-list)}})]
        (:status response) => 200
        (response-mime-type response) => (mime-type :item-list)
        (json? response) => true
        (items-from-body (body-from-response response)) => []))

    ;; all good, 1 item - 200 OK
    ;; curl -i --header "Accept: application/vnd.collection+vnd.fcms.item+json;version=1" --header "Accept-Charset: utf-8" -X GET http://localhost:3000/one/
    (fact "from a collection with one item"
      (let [response (api-request :get "/one/" {:headers {:Accept (mime-type :item-list)}})
            body (body-from-response response)
            items (items-from-body body)
            item (first items)]
          (:status response) => 200
          (response-mime-type response) => (mime-type :item-list)
          (json? response) => true
          (count items) => 1
          (:name item) => i
          (:slug item) => i
          (:description item) => ascii-description
          (:version item) => 1
          (:collection item) => one
          (instance? timestamp (parse (:created-at item))) => true
          (verify-item-links one i (:links item)))
      (collection/item-count one) => 1)

    ;; all good, many items - 200 OK
    ;; curl -i --header "Accept: application/vnd.collection+vnd.fcms.item+json;version=1" --header "Accept-Charset: utf-8" -X GET http://localhost:3000/one/
    (fact "from a collection with many items"
      (let [response (api-request :get "/many/" {:headers {:Accept (mime-type :item-list)}})
            body (body-from-response response)
            items (items-from-body body)
            i-1 (first items)
            uni-i (second items)
            i-2 (nth items 2)
            i-3 (nth items 3)
            i-4 (nth items 4)]
        (:status response) => 200
        (response-mime-type response) => (mime-type :item-list)
        (json? response) => true
        (count items) => 5
        i-1 => (contains {
          :collection many
          :name i
          :slug i
          :description ascii-description
          :version 1})
        (instance? timestamp (parse (:created-at i-1))) => true
        (verify-item-links many i (:links i-1))
        uni-i => (contains {
          :collection many
          :name unicode-name
          :slug "uni-i"
          :description unicode-description
          :version 1})
        (instance? timestamp (parse (:created-at uni-i))) => true
        (verify-item-links many "uni-i" (:links uni-i))
        i-2 => (contains {
          :collection many
          :name "i 2"
          :slug "i-2"
          :description (str ascii-description " 2")
          :version 1})
        (instance? timestamp (parse (:created-at i-2))) => true
        (verify-item-links many "i-2" (:links i-2))
        i-3 => (contains {
          :collection many
          :name "i 3"
          :slug "i-3"
          :description (str ascii-description " 3")
          :version 1})
        (instance? timestamp (parse (:created-at i-3))) => true
        (verify-item-links many "i-3" (:links i-3))
        i-4 => (contains {
          :collection many
          :name "i 4"
          :slug "i-4"
          :description (str ascii-description " 4")
          :version 1})
        (instance? timestamp (parse (:created-at i-4))) => true
        (verify-item-links many "i-4" (:links i-4)))
      (collection/item-count many) => 5)

    ;; all good, force pagination - 200 OK
    (future-fact "when forcing pagination")

    ;; all good, force pagination and using a subsequent page - 200 OK
    (future-fact "when forcing pagination and using a subsequent page")

    ;; no accept - 200 OK
    ;; curl -i --header "Accept-Charset: utf-8" -X GET http://localhost:3000/one/
    (fact "without the Accept header"
      (let [response (api-request :get "/one/" {})
            body (body-from-response response)
            items (items-from-body body)
            item (first items)]
        (:status response) => 200
        (response-mime-type response) => (mime-type :item-list)
        (json? response) => true
        (count items) => 1
        item => (contains {
          :collection one
          :name i
          :slug i
          :description ascii-description
          :version 1})
        (instance? timestamp (parse (:created-at item))) => true
        (verify-item-links one i (:links item))))

    ;; no accept charset - 200 OK
    ;; curl -i --header "Accept: application/vnd.collection+vnd.fcms.item+json;version=1" -X GET http://localhost:3000/one/
    (fact "without the Accept-Charset header"
      (let [response (api-request :get "/one/" {
        :skip-charset true
        :headers {
          :Accept (mime-type :item-list)}})]
        (:status response) => 200
        (response-mime-type response) => (mime-type :item-list)
        (json? response) => true
        (collection/item-count one) => 1
        (let [body (body-from-response response)
              items (items-from-body body)
              item (first items)]
          (count items) => 1
          (:name item) => i
          (:slug item) => i
          (:description item) => ascii-description
          (:version item) => 1
          (:collection item) => one
          (instance? timestamp (parse (:created-at item))) => true
          (verify-item-links one i (:links item))))))

  (facts "about attempting to use the REST API to list items"

    ;; wrong accept - 406 Not Acceptable
    ;; curl -i --header "Accept: application/vnd.fcms.item+json;version=1" --header "Accept-Charset: utf-8" -X GET http://localhost:3000/one/
    (fact "with the wrong Accept header"
      (let [response (api-request :get "/one/" {:headers {:Accept (mime-type :item)}})
            body (body-from-response response)]
        (:status response) => 406
        (response-mime-type response) => (mime-type :text)
        (.contains body "Acceptable media type: application/vnd.collection+vnd.fcms.item+json;version=1") => true
        (.contains body "Acceptable charset: utf-8") => true))

    ;; wrong accept charset - 406 Not Acceptable
    ;; curl -i --header "Accept: application/vnd.collection+vnd.fcms.item+json;version=1" --header "Accept-Charset: iso-8859-1" -X GET http://localhost:3000/one/
    (fact "with the wrong Accept-Charset header"
         (let [response (api-request :get "/one/" {
          :headers {
            :Accept (mime-type :item-list)
            :Accept-Charset "iso-8859-1"}})]
          (:status response) => 406
          (response-mime-type response) => (mime-type :text)
          (let [body (body-from-response response)]
            (.contains body "Acceptable media type: application/vnd.collection+vnd.fcms.item+json;version=1") => true
            (.contains body "Acceptable charset: utf-8") => true)))
    
    ;; collection doesn't exist - 404 Not Found
    ;; curl -i --header "Accept: application/vnd.collection+vnd.fcms.item+json;version=1" --header "Accept-Charset: utf-8" -X GET http://localhost:3000/not-here/
    (fact "from a collection that doesn't exist"
      (let [response (api-request :get "/not-here/" {:headers {:Accept (mime-type :item-list)}})
            body (body-from-response response)]
        (:status response) => 404
        (response-mime-type response) => (mime-type :text)
        (.contains body "Collection not found.") => true))
    
    ;; force pagination and use a subsequent page
    (future-fact "when forcing pagination and using an invalid page")))