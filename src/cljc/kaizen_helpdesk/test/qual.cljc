(ns kaizen-helpdesk.test.qual
  (:require [kaizen-helpdesk.qual :as q]
            #?(:clj  [clojure.test :refer [deftest is testing are]]
               :cljs [cljs.test :refer-macros [deftest is testing are]])
            #?(:clj  [clj-time.core :as t]
               :cljs [cljs-time.core :as t])))

;; Query Qualification Tests

(deftest query-qual
  (testing "Query Qualifications"
    (testing "with simple qualifications"
      (are [expected qual]
          (= expected (q/query-qual-evaluate qual))
        [:= :ticket_id 3]              "(ticket-id  =  3)"
        [:= :custom_field_int -5]      "(custom-field-int = -5)"
        [:!= :custom_field_int -5]     "(custom-field-int != -5)"
        [:= :custom_field_float 4.5]   "(custom-field-float = 4.5)"
        [:> :custom_field_float -8.75] "(custom-field-float > -8.75)"
        [:= :priority "Low"]           "(priority = \"Low\")"
        [:like :location "US-NY-%"]    "(location like \"US-NY-%\")"
        [:not-like :location "US-%"]   "(location not-like \"US-%\")"
        [:= :location nil]             "(location = nil)" ;; location is not set
        [:= :custom_field_bool false]  "(custom-field-bool = false)"
        [:= :custom_field_bool true]   "(custom-field-bool = true)"
        [:> :create_date (t/date-time 2018 9 3 10 0 0 0)]  "(create-date > \"2018-09-03T10:00:00Z\")"
        ))
    (testing "with complex qualifications"
      (are [expected qual]
          (= expected (q/query-qual-evaluate qual))
        [:and [:= :ticket_id 3] [:= :priority "Low"]]      "( ( ticket-id = 3 )   and   ( priority  =  \"Low\" ) )"
        [:or [:= :priority "Low"] [:= :priority "Medium"]] "( ( priority  =  \"Low\" )  or  ( priority  =  \"Medium\" ) )"
        [:and [:= :ticket_id 3] [:= :priority "Low"]]      "((ticket-id = 3) and (priority = \"Low\"))"
        [:or [:= :priority "Low"] [:= :priority "Medium"]] "((priority = \"Low\") or (priority = \"Medium\"))"        
        [:or
         [:= :priority "Low"]
         [:= :priority "Medium"]
         [:= :priority "High"]] "((priority = \"Low\") or (priority = \"Medium\") or (priority = \"High\"))"
        [:and
         [:> :create_date (t/date-time 2018 9 3 10 0 0 0)]
         [:= :status "New"]]  "((create-date > \"2018-09-03T10:00:00Z\") and (status = \"New\"))"))))

;; Request Qualification Tests

(def sample-read-api-request
  {:api-op :read,
   :identity {:user "admin", :exp 1536303866, :type "admin"},
   :paginate {:limit nil, :page 1},
   :payload {:qual "((ticket-id > 3) and (created-on <= \"2018-09-03T23:41:54Z\"))"},
   :entity :ticket})

(deftest request-qual
  (testing "Request Qualifications"
    (testing "with simple qualifications"
      (are [expected qual]
          (= expected (q/request-qual-evaluate qual sample-read-api-request))
        true  "( api-op = :read)"
        false "(api-op = :create)"
        true  "(not (api-op = :create))"
        true  "(identity.user contains \"ad\")"
        false "(identity.user contains \"zzz\")"
        true  "(not (identity.user contains \"zzz\"))"
        false "((api-op = :create) and (entity = :ticket))"
        true  "((not (api-op = :create)) and (entity = :ticket))"))))
