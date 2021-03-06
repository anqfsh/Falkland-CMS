(ns fcms.unit.resources.collection.collection-update
  (:require [midje.sweet :refer :all]
            [clj-time.core :refer (before?)]
            [fcms.lib.resources :refer :all]
            [fcms.lib.check :refer (about-now?)]
            [fcms.resources.collection :refer :all]
            [fcms.resources.common :as common]))

;; ----- Fixtures -----

(def new-name (str ascii-name unicode-name))

;; ----- Tests -----

(with-state-changes [(before :facts (do
                                      (reset-collection e)
                                      (delete-collection c)
                                      (create-collection c {:custom foo})))
                      (after :facts (delete-all-collections))]
  
  (facts "about checking validity of invalid collection updates"

    (fact "when the specified collection doesn't exist"
      (doseq [coll-slug (conj bad-strings "not-here")]
        (valid-collection-update coll-slug {}) => :bad-collection))

    (fact "when updating with a provided slug that is invalid"
      (valid-collection-update c {:slug unicode-name}) => :invalid-slug)

    (fact "when updating with a provided slug that is already used"
      (valid-collection-update c {:slug e}) => :slug-conflict)
    
    (fact "when updating a reserved property"
      (doseq [prop common/reserved-properties]
        (valid-collection-update c {prop foo}) => :property-conflict
        (valid-collection-update c {(name prop) foo}) => :property-conflict)))

  (facts "about checking validity of valid collection updates"

    (fact "with no properties"
      (valid-collection-update c {}) => true)
    
    (fact "when updating the name"
      (valid-collection-update c {:name new-name}) => true)

    (fact "when updating with the same name"
      (valid-collection-update c {:name c}) => true)

    (fact "when updating the slug"
      (valid-collection-update c {:slug slug}) => true)

    (fact "when updating with the same slug"
      (valid-collection-update c {:slug c}) => true)

    (fact "when updating a custom property"
      (valid-collection-update c {:custom bar}) => true)

    (fact "when adding new custom properties"
      (valid-collection-update c {:custom2 foo "custom3" bar}) => true)

    (fact "when updating many things at once"
      (valid-collection-update c {
        :name new-name
        :slug slug
        :custom bar
        :custom2 foo}) => true))

  (facts "about collection update failures"

    (fact "when the specified collection doesn't exist"
      (doseq [coll-slug (conj bad-strings "not-here")]
        (update-collection coll-slug {}) => :bad-collection))

    (fact "with a provided slug that is invalid"
      (update-collection c {:slug "i I"}) => :invalid-slug)

    (fact "with a provided slug that is already used"
      (update-collection c {:slug e}) => :slug-conflict)

    (fact "with a reserved property"
        (doseq [prop common/reserved-properties]
          (update-collection c {prop foo}) => :property-conflict
          (update-collection c {(name prop) foo}) => :property-conflict)))

  (facts "about updating collections"

    (fact "when updating with no properties"
      (update-collection c {})
      (let [collection (get-collection c)]
        (:custom collection) => nil ; custom drops out because it wasn't in the update
        (:version collection) => 2))

    (fact "when updating the name"
      (update-collection c {:name new-name})
      (let [collection (get-collection c)]
        (:name collection) => new-name
        (:version collection) => 2))

    (fact "when updating the slug"
      (update-collection c {:slug slug})
      ; can no longer retrieve the collection by the old slug
      (get-collection c) => nil
      (let [collection (get-collection slug)]
        (:slug collection) => slug
        (:version collection) => 2))

    (fact "when updating a custom property"
      (update-collection c {:custom bar})
      (let [collection (get-collection c)]
        (:custom collection) => bar
        (:version collection) => 2))

    (fact "when updating with new custom properties"
      (update-collection c {:custom2 foo "custom3" bar})
      (let [collection (get-collection c)]
        (:custom collection) => nil ; custom drops out because it wasn't in the update
        (:custom2 collection) => foo
        (:custom3 collection) => bar
        (:version collection) => 2))

    (fact "when updating with multiple updates"
      (update-collection c {:name new-name :slug slug :custom2 foo "custom3" bar}) => truthy
      (let [collection (get-collection slug)]
        (:name collection) => new-name
        (:slug collection) => slug
        (:custom collection) => nil ; drops out because it wasn't in update
        (:custom2 collection) => foo
        (:custom3 collection) => bar
        (:version collection) => 2))

    (fact "about changes to the collection's timestamps"
      (Thread/sleep 1000) ; delay 1 second since timestamps are at second resolution
      (update-collection c {})
      (let [collection (get-collection c)]
        (instance? timestamp (:created-at collection)) => true
        (instance? timestamp (:updated-at collection)) => true
        (about-now? (:updated-at collection)) => true
        (before? (:created-at collection) (:updated-at collection)) => true
        (:created-at (get-collection c)) =not=> (:updated-at (get-collection c))))))