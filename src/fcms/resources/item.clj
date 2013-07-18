(ns fcms.resources.item
  (:require [clojure.core.match :refer (match)]
            [com.ashafa.clutch :as clutch]
            [fcms.resources.common :as common]
            [fcms.resources.collection :as collection]))

(def item-media-type "application/vnd.fcms.item+json;version=1")

(defn- item-doc [coll-id item-slug]
  (:doc (first (clutch/get-view "item" :all {:key [coll-id item-slug] :include_docs true}))))

(defn- item-from-db 
  "Turn an item from its CouchDB map representation into its FCMS map representation."
  [coll-slug item]
  (common/map-from-db (assoc-in item [:data :collection] coll-slug)))

(defn create-item
  "Create a new item in the collection specified by its slug, using the specified
  item name and an optional map of properties. If :slug is included in the properties
  it will be used as the item's slug, otherwise the slug will be created from
  the name."
  ([coll-slug item-name] (create-item coll-slug item-name {}))
  ([coll-slug item-name props]
    (collection/with-collection coll-slug
      (when-let [item (common/create-with-db (merge props {:collection (:id collection) :name item-name}) :item)]
        (item-from-db coll-slug item)))))

(defn get-item
  "Given the slug of the collection containing the item and the slug of the item,
  return the item as a map, or return :bad-collection if there's no collection with that slug, or
  nil if there is no item with that slug."
  [coll-slug item-slug]
    (collection/with-collection coll-slug
      (when-let [item (item-doc (:id collection) item-slug)]
        (item-from-db coll-slug item))))

(defn delete-item
  "Given the slug of the collection containing the item and the slug of the item,
  delete the item, or return :bad-collection if there's no collection with that slug, or
  :bad-item if there is no item with that slug."
  [coll-slug item-slug]
  (if-let [coll-id (:id (collection/get-collection coll-slug))]
    (if-let [item (clutch/with-db (common/db) (item-doc coll-id item-slug))]
      (common/delete item)
      :bad-item)
    :bad-collection))

(defn valid-new-item?
  "Given the slug of the collection, and a map of a potential new item,
  check if the everything is in order to create the new item.
  Ensure the collection exists or return :bad-collection.
  Ensure the name of the item is specified or return :no-name.
  Ensure the slug is valid and doesn't already exist if it's specified,
  or return :invalid-slug or :slug-conflict respectively.
  If no item slug is specified it will be generated from the name."
  ([coll-slug item-name] (valid-new-item? coll-slug item-name {}))
  ([coll-slug item-name {provided-slug :slug}]
    (if-let [coll-id (:id (collection/get-collection coll-slug))]
      (cond
        (not item-name) :no-name
        (not provided-slug) true
        (not (common/valid-slug? provided-slug)) :invalid-slug
        (nil? (get-item coll-slug provided-slug)) true
        :else :slug-conflict)
      :bad-collection)))

(defn valid-item-update?
  "Given the slug of the collection, the slug of the item,
  and a map of updated properties for the item,
  check if the everything is in order to update the item.
  Ensure the collection exists or return :bad-collection.
  Ensure the item exists or return :bad-item.
  If a new slug is provided in the properties, ensure it is
  valid or return :invalid-slug and ensure it is unused or
  return :slug-conflict. If no item slug is specified in
  the properties it will be retain its current slug."
  [coll-slug item-slug {item-name :name provided-slug :slug}]
    (let [coll-id (:id (collection/get-collection coll-slug))
          item-id (:id (get-item coll-slug item-slug))]
      (cond
        (nil? coll-id) :bad-collection
        (nil? item-id) :bad-item
        (not item-name) :no-name
        (not provided-slug) true
        (not (common/valid-slug? provided-slug)) :invalid-slug
        (= item-slug provided-slug) true
        :else (if (nil? (get-item coll-slug provided-slug)) true :slug-conflict))))

(defn- update [coll-slug item-slug updated-props]
  (collection/with-collection coll-slug
    (if-let [item (item-doc (:id collection) item-slug)]
      (let [retained-props (select-keys (:data item) [:type :collection :slug])]
        (item-from-db coll-slug (common/update-with-db item (merge retained-props updated-props))))
      :bad-item)))

(defn update-item
  "Update an item in the collection specified by its slug using the specified
  map of properties. If :slug is included in the properties
  the item will be moved to the new slug, otherwise the slug will remain the same.
  The same validity conditions and invalid return values as valid-item-update? apply."
  [coll-slug item-slug props]
    (let [reason (valid-item-update? coll-slug item-slug props)]
      (if (= reason true)
        (update coll-slug item-slug props)
        reason)))