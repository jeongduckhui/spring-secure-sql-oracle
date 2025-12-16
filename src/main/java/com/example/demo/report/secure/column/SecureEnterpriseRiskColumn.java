package com.example.demo.report.secure.column;

public enum SecureEnterpriseRiskColumn {

    ENTERPRISE_NAME("b.enterprise_name"),
    INDUSTRY_CODE("b.industry_code"),
    TOTAL_REVENUE("ia.total_revenue"),
    AVG_ACTIVE_USERS("ua.avg_active_users"),
    TOTAL_API_CALLS("ua.total_api_calls");

    private final String sql;

    SecureEnterpriseRiskColumn(String sql) {
        this.sql = sql;
    }

    public String selectSql() {
        return sql + " AS " + name().toLowerCase();
    }

    public String rawSql() {
        return sql;
    }

    public static SecureEnterpriseRiskColumn from(String key) {
        try {
            return valueOf(key);
        } catch (Exception e) {
            throw new IllegalArgumentException("허용되지 않은 컬럼: " + key);
        }
    }
}
