(ns kaizen-helpdesk.test.qual
  (:use clojure.test)
  (:require [clj-time.core :as t]
            [kaizen-helpdesk.qual :as q]))

;; Query Qualification Tests

(deftest query-qual
  (testing "Query Qualifications"
    (testing "with simple qualifications"
      (are [expected qual]
          (= expected (q/query-qual-evaluate qual))
        [:= :ticket_id 3]     "(ticket-id = 3)"       
        [:= :priority "Low"]  "(priority = \"Low\")"
        [:> :create_date (t/date-time 2018 9 3 10 0 0 0)]  "(create-date > \"2018-09-03T10:00:00Z\")"
        ))
    (testing "with complex qualifications"
      (are [expected qual]
          (= expected (q/query-qual-evaluate qual))
        [:and [:= :ticket_id 3] [:= :priority "Low"]]      "((ticket-id = 3) and (priority = \"Low\"))"
        [:or [:= :priority "Low"] [:= :priority "Medium"]] "((priority = \"Low\") or (priority = \"Medium\"))"
        [:and
         [:> :create_date (t/date-time 2018 9 3 10 0 0 0)]
         [:= :status "New"]]  "((create-date > \"2018-09-03T10:00:00Z\") and (status = \"New\"))"))))
