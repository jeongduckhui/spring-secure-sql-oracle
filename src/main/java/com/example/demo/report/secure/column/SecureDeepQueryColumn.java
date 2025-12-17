package com.example.demo.report.secure.column;

public enum SecureDeepQueryColumn {

    ENTERPRISE_ID("FINAL.enterprise_id"),
    ENTERPRISE_NAME("FINAL.enterprise_name"),
    INDUSTRY_CODE("FINAL.industry_code"),

    SUBSCRIPTION_ID("FINAL.subscription_id"),

    TOTAL_REVENUE("FINAL.total_revenue");

    private final String sql;

    SecureDeepQueryColumn(String sql) {
        this.sql = sql;
    }

    public String selectSql() {
        return sql + " AS " + name().toLowerCase();
    }

    public String rawSql() {
        return sql;
    }

    public static SecureDeepQueryColumn from(String key) {
        try {
            return valueOf(key);
        } catch (Exception e) {
            throw new IllegalArgumentException("허용되지 않은 컬럼: " + key);
        }
    }
}
