(require '[clojure.string :refer (lower-case blank?)]
         '[clojure.walk :refer (keywordize-keys)]
         '[clj-json.core :as json]
         '[clojure.core.incubator :refer (dissoc-in)]
         '[print.foo :refer (print->)]
         '[ring.mock.request :refer (request body content-type header)]
         '[fcms.lib.http-mock :as http-mock]
         '[fcms.lib.check :refer (check)]
         '[fcms.app :refer (app)]
         '[fcms.resources.collection :refer (collection-media-type)]
         '[fcms.resources.item :refer (item-media-type)])

(defn- method-keyword [method]
  (keyword (lower-case method)))

;; TODO ideally this should be all one mime type, but on an item GET we aren't returning a version #
(defn- send-mime-type [res-type]
  (case res-type
    "item" item-media-type
    "collection" collection-media-type))
(defn- receive-mime-type [res-type]
  (case res-type
    "item" "application/vnd.fcms.item+json"
    "collection" "application/vnd.fcms.collection+json"))

(defn- body-from-response [resp-map]
  (try
    ;; treat the body as JSON
    (keywordize-keys (json/parse-string (:body resp-map)))
    (catch Exception e
      ;; must not be valid JSON
      (:body resp-map))))

;; mock a request

(When #"^I have a \"([^\"]*)\" request to URL \"([^\"]*)\"$" [method url]
  (http-mock/body {})
  (http-mock/request (request (method-keyword method) url))
  (http-mock/request (header (http-mock/request) "Accept-Charset" "utf-8")))

(When #"^I set the \"([^\"]*)\" header to \"([^\"]*)\"$" [header-name value]
  (http-mock/request (header (http-mock/request) header-name value)))

(When #"^I remove the header \"([^\"]*)\"$" [header]
  (http-mock/request (dissoc-in (http-mock/request) [:headers (lower-case header)])))

(When #"^I accept a[n]? \"([^\"]*)\"$" [res-type]
  (http-mock/request (header (http-mock/request) "Accept" (send-mime-type res-type))))

(When #"^I provide a[n]? \"([^\"]*)\"$" [res-type]
  (http-mock/request (content-type (http-mock/request) (send-mime-type res-type))))

(When #"^I provide no body$" []
  (http-mock/body nil))

(When #"^I provide the body \"([^\"]*)\"$" [contents]
  (http-mock/body contents))

(When #"^I set the \"([^\"]*)\" to \"([^\"]*)\"$" [property value]
  (http-mock/body (assoc (http-mock/body) (keyword property) value)))

;; pretends to execute the request, then checks the HTTP status code
(Then #"^the status is \"([^\"]*)\"$" [status]
  (http-mock/response (app (body (http-mock/request) (json/generate-string (http-mock/body)))))
  (check (= (read-string status) (:status (http-mock/response)))))

;; check on the response

(Then #"^the \"([^\"]*)\" header is \"([^\"]*)\"$" [header value]
  (check (= value (get-in (http-mock/response) [:headers header]))))

(Then #"^the \"([^\"]*)\" header is not present$" [header]
  (check (nil? (get-in (http-mock/response) [:headers header]))))

(Then #"^the body is JSON$" []
  (http-mock/body (body-from-response (http-mock/response)))
  (check (map? (http-mock/body))))

(Then #"^I receive a[n]? \"([^\"]*)\"$" [res-type]
  (check (.contains (get-in (http-mock/response) [:headers "Content-Type"]) (receive-mime-type res-type))))

(Then #"^the body is text$" []
  (http-mock/body (body-from-response (http-mock/response)))
  (check (string? (http-mock/body))))

(Then #"^the body is empty$" []
  (http-mock/body (body-from-response (http-mock/response)))
  (check (blank? (http-mock/body))))

(Then #"^the body is \"([^\"]*)\"$" [contents]
  (check (string? (http-mock/body)))
  (check (= (http-mock/body) contents)))

(Then #"^the body contains \"([^\"]*)\"$" [contents]
  (check (string? (http-mock/body)))
  (check (.contains (http-mock/body) contents)))

(Then #"^the \"([^\"]*)\" is \"([^\"]*)\"$" [property value]
  (check (map? (http-mock/body)))
  (check (= value ((http-mock/body) (keyword property)))))

(Then #"^the \"([^\"]*)\" does not exist$" [property]
  (check (map? (http-mock/body)))
  (check (not (contains? (http-mock/body) (keyword property)))))