(ns fcms.api.items
  (:require [clojure.core.match :refer (match)]
            [compojure.core :refer (defroutes ANY)]
            [liberator.core :refer (defresource by-method)]
            [liberator.representation :refer (ring-response)]
            [taoensso.timbre :refer (debug info warn error fatal spy)]
            [fcms.api.common :as common]
            [fcms.resources.collection :as collection]
            [fcms.resources.item :as item]
            [fcms.representations.items :refer (render-item render-items)]))

(def missing-item-response
  (ring-response
    {:status 404
     :headers {"Content-Type" "text/plain"}}))

(defn- get-item [coll-slug item-slug]
  (let [item (item/get-item coll-slug item-slug)]
    (match item
      :bad-collection [false {:bad-collection true}]
      nil false
      :else {:item item})))

(defn- get-items [coll-slug]
  (if-let [collection (collection/get-collection coll-slug)]
    [true {:items (item/all-items coll-slug)}]
    [false {:bad-collection true}]))

(defn- item-location-response [coll-slug item]
  (common/location-response [coll-slug (:slug item)] (render-item item) item/item-media-type))

;; Create new item

(defn- create-item [coll-slug item]
  (when-let [item (item/create-item coll-slug (:name item) item)]
    {:item item}))

(defn- unprocessable-reason [reason]
  (match reason
    :bad-collection common/missing-collection-response
    :bad-item missing-item-response
    :no-name "Name is required."
    :slug-conflict "Slug already used in collection."
    :invalid-slug "Invalid slug."
    :else "Not processable."))

;; Update item

(defn- update-item [coll-slug item-slug item]
  (when-let [result (item/update-item coll-slug item-slug item)]
    {:updated-item result}))

(defn- update-item-response [coll-slug ctx]
  (if (= (get-in ctx [:updated-item :slug]) (get-in ctx [:item :slug]))
    ; it's in the same spot
    (render-item (:updated-item ctx))
    ; it moved
    (item-location-response coll-slug (:updated-item ctx))))

;; Resources, see: http://clojure-liberator.github.io/liberator/assets/img/decision-graph.svg

(defresource item [coll-slug item-slug]
  :available-charsets [common/UTF8]
  :available-media-types [item/item-media-type]
  :handle-not-acceptable (fn [_] (common/only-accept item/item-media-type))
  :allowed-methods [:get :put :delete]
  :exists? (fn [_] (get-item coll-slug item-slug))
  :handle-not-found (fn [ctx] (when (:bad-collection ctx) common/missing-collection-response))
  ;; Get an item
  :handle-ok (by-method {
    :get (fn [ctx] (render-item (:item ctx)))
    :post (fn [ctx] (render-item (:item ctx)))
    :put (fn [ctx] (update-item-response coll-slug ctx))})
  ;; Delete an item
  :delete! (fn [_] (item/delete-item coll-slug item-slug))
  ;; Update/Create an item
  :malformed? (by-method {
    :get false
    :delete false
    :post (fn [ctx] (common/malformed-json? ctx))
    :put (fn [ctx] (common/malformed-json? ctx))})
  :known-content-type? (fn [ctx] (common/known-content-type ctx item/item-media-type))
  :handle-unsupported-media-type (fn [ctx] (common/only-accept item/item-media-type))
  :processable? (by-method {
    :get true
    :delete true
    :post (fn [ctx] (common/check-input (item/valid-item-update? coll-slug item-slug (:data ctx))))
    :put (fn [ctx] (common/check-input (item/valid-item-update? coll-slug item-slug (:data ctx))))})
  :handle-unprocessable-entity (fn [ctx] (unprocessable-reason (:reason ctx)))
  :can-put-to-missing? (fn [_] false) ; temporarily only use PUT for update
  :conflict? (fn [_] false)
  :put! (fn [ctx] (update-item coll-slug item-slug (:data ctx)))
  :new? (by-method {:post true :put false})
  :respond-with-entity? (by-method {:put true :delete false})
  :handle-not-implemented (fn [ctx] (when (:bad-collection ctx) common/missing-collection-response)))

(defresource items-list [coll-slug]
  :available-charsets [common/UTF8]
  :available-media-types [item/item-media-type]
  :handle-not-acceptable (fn [ctx] (common/only-accept item/item-media-type))
  :allowed-methods [:get :post]
  :exists? (fn [ctx] (get-items coll-slug))
  ;; Get list of items
  :handle-ok (fn [ctx] (render-items coll-slug (:items ctx)))
  ;; Create new item
  :malformed? (by-method {
    :get false
    :post (fn [ctx] (common/malformed-json? ctx))})
  :known-content-type? (fn [ctx] (common/known-content-type ctx item/item-media-type))
  :handle-unsupported-media-type (fn [ctx] (common/only-accept item/item-media-type))
  :processable? (by-method {
    :get true
    :post (fn [ctx] (common/check-input (item/valid-new-item? coll-slug (get-in ctx [:data :name]) (:data ctx))))})
  :handle-unprocessable-entity (fn [ctx] (unprocessable-reason (:reason ctx)))
  :post! (fn [ctx] (create-item coll-slug (:data ctx)))
  :handle-created (fn [ctx] (item-location-response coll-slug (:item ctx))))

;; Routes

(defroutes item-routes
  (ANY "/:coll-slug/:item-slug" [coll-slug item-slug] (item coll-slug item-slug))
  (ANY "/:coll-slug/" [coll-slug] (items-list coll-slug)))