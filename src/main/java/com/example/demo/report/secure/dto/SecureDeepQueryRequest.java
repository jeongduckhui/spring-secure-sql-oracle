package com.example.demo.report.secure.dto;

import java.util.List;

public class SecureDeepQueryRequest {

    private List<String> selectColumns;
    private List<String> groupByColumns;
    private List<String> orderByColumns;
    private List<String> inConditionColumns;

    private List<Long> enterpriseIds;
    private String fromDate;
    private String toDate;
    private Long minRevenue;
    private String orderByDir;

    public List<String> getSelectColumns() { return selectColumns; }
    public List<String> getGroupByColumns() { return groupByColumns; }
    public List<String> getOrderByColumns() { return orderByColumns; }
    public List<String> getInConditionColumns() { return inConditionColumns; }

    public List<Long> getEnterpriseIds() { return enterpriseIds; }
    public String getFromDate() { return fromDate; }
    public String getToDate() { return toDate; }
    public Long getMinRevenue() { return minRevenue; }
    public String getOrderByDir() { return orderByDir; }
}
