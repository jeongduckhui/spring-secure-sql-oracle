package com.example.demo.report.enums;

/**
 * ORDER BY 전용 enum
 * - SELECT enum과 완전히 분리
 */
public enum SalesReportOrderBy {

    REGION_NAME("r.region_name"),
    PORTFOLIO_VALUE("portfolio_value"),
    AVG_RISK_SCORE("avg_risk_score"),
    REP_TENURE("avg_rep_tenure_months");

    private final String sql;

    SalesReportOrderBy(String sql) {
        this.sql = sql;
    }

    public String sql() {
        return sql;
    }

    public static SalesReportOrderBy fromKey(String key) {
        return valueOf(key);
    }
}
