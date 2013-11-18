(ns fcms.lib.resources
  (:require [clojure.test :refer :all]
  					[clj-time.format :refer (parse)]
            [fcms.resources.collection-resource :as resource]
  					[fcms.resources.collection :as collection]
            [fcms.resources.taxonomy :as taxonomy]
            [fcms.resources.item :as item]))

;; ----- Fixtures -----

(def c "c")
(def e "e")
(def t "t")
(def t2 "food")
(def foo "foo")

(def existing-categories-t [
  {:slug "foo" :name "Foo"}
  {:slug "bar" :name "Bar"}
  {:slug "fubar" :name "FUBAR" :categories [
    {:slug "a" :name "A"}
    {:slug "b" :name "B"}
  ]}])

(def existing-categories-t2 [
  {:slug "fruit" :name "Fruit" :categories [
    {:slug "apple" :name "Apple"}
    {:slug "pear" :name "Pear"}
  ]}
  {:slug "vegetable" :name "Vegetable" :categories [
    {:slug "carrot" :name "Carrot"}
    {:slug "brocolli" :name "Brocolli"}
  ]}])

(defn empty-collection-c [f]
  (collection/delete-collection c)
  (collection/create-collection c)
  (f)
  (collection/delete-collection c))

(defn existing-taxonomy-t [f]
  (resource/create-resource c "Taxonomy" :taxonomy [] 
    {:slug t
     :description "Categorize it."
     :categories existing-categories-t})
  (f)
  (taxonomy/delete-taxonomy c t))

(defn existing-taxonomy-t2 [f]
  (resource/create-resource c "Food" :taxonomy [] 
    {:slug t2
     :description "Yummy."
     :categories existing-categories-t2})
  (f)
  (taxonomy/delete-taxonomy c t2))

(defn existing-item-e [f]
  (item/create-item c e)
  (taxonomy/categorize-item c e "t/foo")
  (taxonomy/categorize-item c e "t/fubar/a")
  (f)
  (item/delete-item c e))

;; ----- Validation functions -----

(defn verify-new-resource [coll-slug resource]
	(is (= (:collection resource) c))
  (is (= (:version resource) 1))
  (is (instance? String (:id resource)))
  (is (instance? String (:created-at resource)))
  (is (instance? org.joda.time.DateTime (parse (:created-at resource))))
  (is (= (:created-at resource) (:updated-at resource))))