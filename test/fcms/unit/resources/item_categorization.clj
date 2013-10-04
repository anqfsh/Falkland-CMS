(ns fcms.unit.resources.item-categorization
  (:require [clojure.test :refer :all]
            [fcms.lib.resource :refer (empty-collection-c)]
            [fcms.resources.collection-resource :as resource]
            [fcms.resources.taxonomy :as taxonomy]
						[fcms.resources.item :as item]))

(def c "c")
(def t "t")
(def i "i")
(def e "e")
(def u "u")
(def foo "foo")

(def existing-categories [
  {:slug "foo" :name "Foo"}
  {:slug "bar" :name "Bar"}
  {:slug "fubar" :name "FUBAR" :categories [
    {:slug "a" :name "A"}
    {:slug "b" :name "B"}
  ]}])

(defn existing-taxonomy-t [f]
  (resource/create-resource c "Taxonomy" :taxonomy [] 
    {:slug t
     :description "Categorize it."
     :categories existing-categories})
  (f)
  (taxonomy/delete-taxonomy c t))

(defn existing-item-i [f]
  (item/create-item c i)
  (f)
  (item/delete-item c i))

(defn existing-item-e [f]
  (item/create-item c e)
  (taxonomy/categorize-item c "t/foo" e)
  (taxonomy/categorize-item c "t/fubar/a" e)
  (f)
  (item/delete-item c e))

(defn existing-item-u []
  (item/create-item c u)
  (taxonomy/categorize-item c "t/foo" u)
  (taxonomy/categorize-item c "t/bar" u)
  (taxonomy/categorize-item c "t/fubar/a" u)
  (taxonomy/categorize-item c "t/fubar/b" u))

(use-fixtures :each empty-collection-c existing-taxonomy-t existing-item-i existing-item-e)

(defn- categorize [expectation coll-slug category-paths item-slug]
	(is (= expectation (taxonomy/categorize-item coll-slug (first category-paths) item-slug)))
	(let [remaining-paths (rest category-paths)]
		(if-not (empty? remaining-paths) (recur expectation coll-slug remaining-paths item-slug))))

(defn- categorize-i [expectation coll-slug category-paths]
	(categorize expectation coll-slug category-paths i))

(defn- categorize-e [expectation coll-slug category-paths]
	(categorize expectation coll-slug category-paths e))

(defn- categorize-foo
  "Create a new item and categorize it by each category in the vector, validating with each add and then with a
  final check on the categories after getting the item."
  ([category-paths category-validations] (categorize-foo true category-paths category-validations))
  ([incremental-validation category-paths category-validations] 
    (item/create-item c foo)
    (categorize-foo incremental-validation category-paths [] category-validations []))
  ([incremental-validation category-paths added-paths category-validations added-validations]
    (let [category (first category-paths)
          validation (first category-validations)
          validations (vec (conj added-validations validation))]
      (if category
        (do
          (is (= (:categories (taxonomy/categorize-item c category foo)) validations))
          (categorize-foo incremental-validation (rest category-paths)
            (vec (conj added-paths category)) (rest category-validations) validations))
        (do
          (is (= (:categories (item/get-item c foo)) added-validations))
          (item/delete-item c foo))))))

(defn- uncategorize-u 
  "Create a new categorized item and uncategorize it by each category in the vector, validating with a
  final check on the categories after getting the item."
  ([expectation coll-slug category-paths] 
    (existing-item-u)
    (uncategorize-u expectation coll-slug (set (map taxonomy/normalize-category-path category-paths)) category-paths)
    (item/delete-item c u))
  ([expectation coll-slug removed-paths category-paths]
    (is (= expectation (taxonomy/uncategorize-item coll-slug (first category-paths) u)))
    (let [remaining-paths (rest category-paths)]
      (if (empty? remaining-paths)
        (is (not-any? removed-paths (:categories (item/get-item coll-slug u))))
        (recur expectation coll-slug removed-paths remaining-paths)))))

(deftest item-categorization
  (testing "item categorization failures"

  	(testing "item categorization with a non-existent collection"
  		(categorize-i :bad-collection "not-here" ["/t/foo"]))

  	(testing "item categorization with a non-existent taxonomy"
  		(categorize-i :bad-taxonomy c ["not-here/foo" "not-here/foo/" "/not-here/foo" "/not-here/foo/"]))

  	(testing "item categorization with a non-existent item"
  		(is (= :bad-item (taxonomy/categorize-item c "/t/foo" "not-here"))))

  	(testing "item categorization with no category"
  		(categorize-i :bad-category c ["t" "t/" "/t" "/t/"]))

  	(testing "item categorization with a non-existent root category"
  		(categorize-i :bad-category c ["t/not-here" "t/not-here/" "/t/not-here" "/t/not-here/"]))

  	(testing "item categorization with a non-existent leaf category"
  		(categorize-i :bad-category c ["t/foo/not-here" "t/foo/not-here/" "/t/foo/not-here" "/t/foo/not-here/"])
  		(categorize-i :bad-category c ["t/fubar/a/not-here" "t/fubar/a/not-here/" "/t/fubar/a/not-here" "/t/fubar/a/not-here/"]))

  	(testing "item categorization with a duplicate category"
  		(categorize-e :duplicate-category c ["t/foo" "t/foo/" "/t/foo" "/t/foo/"])
  		(categorize-e :duplicate-category c ["t/fubar" "t/fubar/" "/t/fubar" "/t/fubar/"])
  		(categorize-e :duplicate-category c ["t/fubar/a" "t/fubar/a/" "/t/fubar/a" "/t/fubar/a/"])))

  (testing "item categorization sucesses"

  	(testing "item categorization with a single root category"
      (categorize-foo ["t/bar"] ["t/bar"])
      (categorize-foo ["t/bar/"] ["t/bar"])
      (categorize-foo ["/t/bar"] ["t/bar"])
      (categorize-foo ["/t/bar/"] ["t/bar"]))

  	(testing "item categorization with multiple root categories"
      (categorize-foo ["t/bar" "t/foo"] ["t/bar" "t/foo"])
      (categorize-foo ["t/bar/" "t/foo/"] ["t/bar" "t/foo"])
      (categorize-foo ["/t/bar" "/t/foo"] ["t/bar" "t/foo"])
      (categorize-foo ["/t/bar/" "/t/foo/"] ["t/bar" "t/foo"]))

  	(testing "item categorization with a single leaf category"
      (categorize-foo ["t/fubar/a"] ["t/fubar/a"])
      (categorize-foo ["t/fubar/a/"] ["t/fubar/a"])
      (categorize-foo ["/t/fubar/a"] ["t/fubar/a"])
      (categorize-foo ["/t/fubar/a/"] ["t/fubar/a"]))

  	(testing "item categorization with multiple leaf categories"
      (categorize-foo ["t/fubar/a" "t/fubar/b"] ["t/fubar/a" "t/fubar/b"])
      (categorize-foo ["t/fubar/a/" "t/fubar/b/"] ["t/fubar/a" "t/fubar/b"])
      (categorize-foo ["/t/fubar/a" "/t/fubar/b"] ["t/fubar/a" "t/fubar/b"])
      (categorize-foo ["/t/fubar/a/" "/t/fubar/b/"] ["t/fubar/a" "t/fubar/b"]))

  	(testing "item categorization with a single root category and a single leaf category"
      (categorize-foo ["t/bar" "t/fubar/b"] ["t/bar" "t/fubar/b"])
      (categorize-foo ["t/bar/" "t/fubar/b/"] ["t/bar" "t/fubar/b"])
      (categorize-foo ["/t/bar" "/t/fubar/b"] ["t/bar" "t/fubar/b"])
      (categorize-foo ["/t/bar/" "/t/fubar/b/"] ["t/bar" "t/fubar/b"]))))

(deftest item-uncategorization
  (testing "item uncategorization failures"

    (testing "item uncategorization with a non-existent collection"
      (uncategorize-u :bad-collection "not-here" ["/t/foo"]))

    (testing "item uncategorization with a non-existent taxonomy"
      (uncategorize-u :bad-taxonomy c ["not-here/foo" "not-here/foo/" "/not-here/foo" "/not-here/foo/"]))

    (testing "item uncategorization with a non-existent item"
      (is (= :bad-item (taxonomy/uncategorize-item c "/t/foo" "not-here"))))

    (testing "item categorization with no category"
      (uncategorize-u :bad-category c ["t" "t/" "/t" "/t/"]))

    (testing "item categorization with a non-existent root category"
      (uncategorize-u :bad-category c ["t/not-here" "t/not-here/" "/t/not-here" "/t/not-here/"]))

    (testing "item uncategorization with a non-existent leaf category"
      (uncategorize-u :bad-category c ["t/foo/not-here" "t/foo/not-here/" "/t/foo/not-here" "/t/foo/not-here/"])
      (uncategorize-u :bad-category c ["t/fubar/a/not-here" "t/fubar/a/not-here/" "/t/fubar/a/not-here" "/t/fubar/a/not-here/"]))

    (testing "item uncategorization with a partial category path"
      (uncategorize-u :bad-category c ["t/fubar" "t/fubar/" "/t/fubar" "/t/fubar/"])))

  (testing "item uncategorization successes"

    (testing "item uncategorization with a single root category"
      (uncategorize-u true c ["t/foo"])
      (uncategorize-u true c ["t/foo/"])
      (uncategorize-u true c ["/t/foo"])
      (uncategorize-u true c ["/t/foo/"]))

    (testing "item uncategorization with multiple root categories"
      (uncategorize-u true c ["t/foo" "t/bar"])
      (uncategorize-u true c ["t/foo/" "t/bar/"])
      (uncategorize-u true c ["/t/foo" "/t/bar"])
      (uncategorize-u true c ["/t/foo/" "/t/bar/"]))

    (testing "item uncategorization with a single leaf category"
      (uncategorize-u true c ["t/fubar/a"])
      (uncategorize-u true c ["t/fubar/a/"])
      (uncategorize-u true c ["/t/fubar/a"])
      (uncategorize-u true c ["/t/fubar/a/"]))

    (testing "item uncategorization with multiple leaf categories"
      (uncategorize-u true c ["t/fubar/a" "t/fubar/b"])
      (uncategorize-u true c ["t/fubar/a/" "t/fubar/b/"])
      (uncategorize-u true c ["/t/fubar/a" "/t/fubar/b"])
      (uncategorize-u true c ["/t/fubar/a/" "/t/fubar/b/"]))

    (testing "item uncategorization with a single root category and a single leaf category"
      (uncategorize-u true c ["t/foo" "t/fubar/a"])
      (uncategorize-u true c ["t/foo/" "t/fubar/a/"])
      (uncategorize-u true c ["/t/foo" "/t/fubar/a"])
      (uncategorize-u true c ["/t/foo/" "/t/fubar/a/"]))))