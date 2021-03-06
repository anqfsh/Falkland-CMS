(ns fcms.api.items
  (:require [compojure.core :refer (defroutes ANY)]
            [liberator.core :refer (defresource by-method)]
            [fcms.api.common :as common]
            [fcms.resources.collection :as collection]
            [fcms.resources.item :as item]
            [fcms.representations.items :refer (render-item render-items)]))

;; ----- Responses -----

(defn- item-location-response [coll-slug item]
  (common/location-response [coll-slug (:slug item)] (render-item item) item/item-media-type))

(defn- unprocessable-reason [reason]
  (case reason
    :bad-collection common/missing-collection-response
    :bad-item common/missing-response
    :no-name (common/unprocessable-entity-response "Name is required.")
    :property-conflict (common/unprocessable-entity-response "A reserved property was used.")
    :slug-conflict (common/unprocessable-entity-response "Slug already used in the collection.")
    :invalid-slug (common/unprocessable-entity-response "Invalid slug.")
    (common/unprocessable-entity-response "Not processable.")))

;; ----- Get items -----

(defn- get-item [coll-slug item-slug]
  (let [item (item/get-item coll-slug item-slug)]
    (case item
      :bad-collection [false {:bad-collection true}]
      nil false
      {:item item})))

(defn- get-items [coll-slug]
  (if (collection/get-collection coll-slug)
    [true {:items (item/all-items coll-slug)}]
    [false {:bad-collection true}]))

;; ----- Create a new item -----

(defn- create-item [coll-slug item]
  (when-let [new-item (item/create-item coll-slug (:name item) item)]
    {:item new-item}))

;; ----- Update an item -----

(defn- update-item [coll-slug item-slug item]
  (when-let [result (item/update-item coll-slug item-slug item)]
    {:updated-item result}))

(defn- update-item-response [coll-slug ctx]
  (if (= (get-in ctx [:updated-item :slug]) (get-in ctx [:item :slug]))
    ; it's in the same spot
    (render-item (:updated-item ctx))
    ; it moved
    (item-location-response coll-slug (:updated-item ctx))))

;; ----- Resources -----
;; see: http://clojure-liberator.github.io/liberator/assets/img/decision-graph.svg

(def item-resource-config {
  :available-charsets [common/UTF8]
  :handle-not-found (fn [ctx] (when (:bad-collection ctx) common/missing-collection-response))
  :handle-unprocessable-entity (fn [ctx] (unprocessable-reason (:reason ctx)))
})

(defresource item [coll-slug item-slug]
  item-resource-config
  :available-media-types [item/item-media-type]
  :handle-not-acceptable (fn [_] (common/only-accept 406 item/item-media-type))
  :allowed-methods [:get :put :delete]
  :exists? (fn [_] (get-item coll-slug item-slug))
  :known-content-type? (fn [ctx] (common/known-content-type? ctx item/item-media-type))
  :handle-unsupported-media-type (fn [_] (common/only-accept 415 item/item-media-type))
  :respond-with-entity? (by-method {:put true :delete false})

  :processable? (by-method {
    :get true
    :delete true
    :put (fn [ctx] (common/check-input (item/valid-item-update coll-slug item-slug (:data ctx))))})

  :handle-ok (by-method {
    :get (fn [ctx] (render-item (:item ctx)))
    :put (fn [ctx] (update-item-response coll-slug ctx))})

  ;; Delete an item
  :delete! (fn [_] (item/delete-item coll-slug item-slug))

  ;; Update an item
  :new? (by-method {:post true :put false})
  :malformed? (by-method {
    :get false
    :delete false
    :post (fn [ctx] (common/malformed-json? ctx))
    :put (fn [ctx] (common/malformed-json? ctx))})
  :can-put-to-missing? (fn [_] false) ; temporarily only use PUT for update
  :conflict? (fn [_] false)
  :put! (fn [ctx] (update-item coll-slug item-slug (:data ctx)))
  :handle-not-implemented (fn [ctx] (when (:bad-collection ctx) common/missing-collection-response)))

(defresource items-list [coll-slug]
  item-resource-config
  :available-media-types (by-method {
    :get [item/item-collection-media-type]
    :post [item/item-media-type]})
  :handle-not-acceptable (by-method {
    :get (fn [_] (common/only-accept 406 item/item-collection-media-type))
    :post (fn [_] (common/only-accept 406 item/item-media-type))})
  :allowed-methods [:get :post]

  ;; Get a list of items
  :exists? (fn [_] (get-items coll-slug))
  :handle-ok (fn [ctx] (render-items coll-slug (:items ctx)))

  ;; Create a new item
  :malformed? (by-method {
    :get false
    :post (fn [ctx] (common/malformed-json? ctx))})
  :known-content-type? (by-method {
    :get (fn [ctx] (common/known-content-type? ctx item/item-collection-media-type))
    :post (fn [ctx] (common/known-content-type? ctx item/item-media-type))})
  :handle-unsupported-media-type (by-method {
    :get (fn [_] (common/only-accept 415 item/item-collection-media-type))
    :post (fn [_] (common/only-accept 415 item/item-media-type))})
  :processable? (by-method {
    :get true
    :post (fn [ctx] (common/check-input (item/valid-new-item coll-slug (get-in ctx [:data :name]) (:data ctx))))})
  :post! (fn [ctx] (create-item coll-slug (:data ctx)))
  :handle-created (fn [ctx] (item-location-response coll-slug (:item ctx))))

;; ----- Routes -----

(defroutes item-routes
  (ANY "/:coll-slug/:item-slug" [coll-slug item-slug] (item coll-slug item-slug))
  (ANY "/:coll-slug/" [coll-slug] (items-list coll-slug)))