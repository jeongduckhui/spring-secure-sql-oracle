package com.example.demo.report.secure.dto;

import java.util.List;

public class SecureEnterpriseRiskRequest {

    private List<String> columns;
    private String enterpriseIds;
    private OrderBy orderBy;

    public static class OrderBy {
        private String key;
        private String dir;

        public String getKey() { return key; }
        public String getDir() { return dir; }
    }

    public List<String> getColumns() { return columns; }
    public String getEnterpriseIds() { return enterpriseIds; }
    public OrderBy getOrderBy() { return orderBy; }
}
