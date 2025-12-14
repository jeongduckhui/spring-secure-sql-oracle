package com.example.demo.securesql.validator;

import java.util.ArrayList;
import java.util.List;

/**
 * 간단한 SQL Injection fuzz 테스트 유틸리티.
 * DB 연결 없이 OracleValidator.validate 만 호출한다.
 */
public class SqlFuzzTester {

    public static class FuzzResult {
        private String sql;
        private boolean passed;
        private String message;

        public FuzzResult(String sql, boolean passed, String message) {
            this.sql = sql;
            this.passed = passed;
            this.message = message;
        }

        public String getSql() { return sql; }
        public boolean isPassed() { return passed; }
        public String getMessage() { return message; }
    }

    public static List<FuzzResult> runDefaultFuzz() {
        List<FuzzResult> results = new ArrayList<>();

        List<String> candidates = List.of(
                // 정상 쿼리 (통과해야 함)
                "SELECT ST.STORE_ID, ST.PRODUCT_ID FROM SALES_TRANSACTION ST",

                // 대표적인 SQL Injection 패턴들 (막혀야 함)
                "SELECT * FROM SALES_TRANSACTION ST WHERE ST.STORE_ID = '1' OR '1'='1'",
                "SELECT ST.STORE_ID FROM SALES_TRANSACTION ST; DROP TABLE USERS",
                "SELECT ST.STORE_ID FROM SALES_TRANSACTION ST WHERE ST.STORE_ID IN (SELECT STORE_ID FROM SALES_TRANSACTION WHERE 'a'='a')",
                "SELECT ST.STORE_ID FROM SALES_TRANSACTION ST WHERE ST.STORE_ID = '1' UNION SELECT USERNAME FROM DBA_USERS"
        );

        for (String sql : candidates) {
            try {
                OracleValidator.validate(sql);
                results.add(new FuzzResult(sql, true, "OK"));
            } catch (Exception e) {
                results.add(new FuzzResult(sql, false, e.getMessage()));
            }
        }

        return results;
    }
}
