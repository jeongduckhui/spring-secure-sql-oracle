-- =========================
-- REGIONS
-- =========================
INSERT INTO regions (region_id, region_name, country_code) VALUES (1, 'Seoul', 'KR');
INSERT INTO regions (region_id, region_name, country_code) VALUES (2, 'Busan', 'KR');
INSERT INTO regions (region_id, region_name, country_code) VALUES (3, 'Tokyo', 'JP');
INSERT INTO regions (region_id, region_name, country_code) VALUES (4, 'New York', 'US');

-- =========================
-- SALES_REPS
-- =========================
INSERT INTO sales_reps VALUES (101, 'Alice Kim', 'Senior Rep', DATE '2018-03-01', 1);
INSERT INTO sales_reps VALUES (102, 'Brian Lee', 'Junior Rep', DATE '2021-06-15', 1);
INSERT INTO sales_reps VALUES (103, 'Chris Park', 'Manager', DATE '2015-01-10', 2);
INSERT INTO sales_reps VALUES (104, 'Diana Choi', 'Senior Rep', DATE '2019-09-05', 3);
INSERT INTO sales_reps VALUES (105, 'Eric Smith', 'Director', DATE '2012-11-20', 4);

-- =========================
-- CUSTOMER_FINANCIALS
-- =========================
INSERT INTO CustomerFinancials VALUES (1001, 101, 1, 15000, 500, 0, 2);
INSERT INTO CustomerFinancials VALUES (1002, 101, 2, 8000, 300, 1, 5);
INSERT INTO CustomerFinancials VALUES (1003, 101, 1, 25000, 700, 0, 1);
INSERT INTO CustomerFinancials VALUES (1004, 102, 3, 5000, 200, 2, 10);
INSERT INTO CustomerFinancials VALUES (1005, 102, 2, 12000, 450, 0, 3);
INSERT INTO CustomerFinancials VALUES (1006, 103, 1, 40000, 1200, 0, 0);
INSERT INTO CustomerFinancials VALUES (1007, 103, 1, 38000, 1100, 1, 8);
INSERT INTO CustomerFinancials VALUES (1008, 103, 2, 9000, 350, 0, 4);
INSERT INTO CustomerFinancials VALUES (1009, 104, 1, 22000, 800, 0, 2);
INSERT INTO CustomerFinancials VALUES (1010, 104, 3, 6000, 250, 3, 15);
INSERT INTO CustomerFinancials VALUES (1011, 105, 1, 70000, 2000, 0, 0);
INSERT INTO CustomerFinancials VALUES (1012, 105, 2, 30000, 950, 1, 6);
INSERT INTO CustomerFinancials VALUES (1013, 105, 1, 45000, 1500, 0, 1);

-- =========================
-- CHURN_SIGNALS
-- =========================
INSERT INTO ChurnSignals VALUES (1001, 0);
INSERT INTO ChurnSignals VALUES (1002, 1);
INSERT INTO ChurnSignals VALUES (1003, 0);
INSERT INTO ChurnSignals VALUES (1004, 3);
INSERT INTO ChurnSignals VALUES (1005, 0);
INSERT INTO ChurnSignals VALUES (1006, 0);
INSERT INTO ChurnSignals VALUES (1007, 2);
INSERT INTO ChurnSignals VALUES (1008, 1);
INSERT INTO ChurnSignals VALUES (1009, 0);
INSERT INTO ChurnSignals VALUES (1010, 4);
INSERT INTO ChurnSignals VALUES (1011, 0);
INSERT INTO ChurnSignals VALUES (1012, 1);
INSERT INTO ChurnSignals VALUES (1013, 0);


INSERT INTO ENTERPRISES VALUES
(1, 'Alpha Corp', 'IT'),
(2, 'Beta Finance', 'FIN');

INSERT INTO SUBSCRIPTIONS VALUES
(100, 1),
(101, 1),
(200, 2);

INSERT INTO INVOICES VALUES
(1, 100, 50000),
(2, 100, 70000),
(3, 101, 30000),
(4, 200, 90000);

INSERT INTO USAGE_LOGS VALUES
(1, 100, 120, 10000),
(2, 100, 130, 12000),
(3, 101, 80, 6000),
(4, 200, 200, 25000);


