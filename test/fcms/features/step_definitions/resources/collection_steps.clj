(require '[fcms.resources.collection :as collection]
         '[fcms.resources.item :as item]
         '[fcms.lib.checks :refer (check)])

(Given #"^I had an empty collection \"([^\"]*)\"$" [coll-slug]
  (collection/delete-collection coll-slug)
  (collection/create-collection coll-slug))

(Given #"^I had a collection \"([^\"]*)\" with the following items?:$" [coll-slug table]
  (collection/delete-collection coll-slug)
  (collection/create-collection coll-slug)
  (let [items (table->rows table)]
    (doseq [item items]
      (item/create-item coll-slug (:name item) {:slug (:slug item) :description (:description item)}))))

(Then #"^the collection \"([^\"]*)\" (had|has|will have) an item count of (\d+)$" [coll-slug _ item-count]
  (check (= (read-string item-count) (collection/item-count coll-slug))))