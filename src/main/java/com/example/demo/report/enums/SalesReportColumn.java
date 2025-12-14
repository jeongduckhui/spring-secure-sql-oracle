package com.example.demo.report.enums;

/**
 * SELECT + GROUP BY 컬럼 enum
 */
public enum SalesReportColumn {

    REGION_NAME("r.region_name", false),
    COUNTRY_CODE("r.country_code", false),
    REP_NAME("sr.rep_name", false),
    JOB_TITLE("sr.job_title", false),
    REVENUE_TIER("cf.revenue_tier", false),
    INDUSTRY_TYPE("cf.industry_type", false);

    private final String sql;
    private final boolean aggregate;

    SalesReportColumn(String sql, boolean aggregate) {
        this.sql = sql;
        this.aggregate = aggregate;
    }

    public String sql() {
        return sql;
    }

    public boolean isAggregate() {
        return aggregate;
    }

    public static SalesReportColumn fromKey(String key) {
        return valueOf(key);
    }
}
