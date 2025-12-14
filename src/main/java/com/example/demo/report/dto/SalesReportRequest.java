package com.example.demo.report.dto;

import java.util.List;

public class SalesReportRequest {

    private List<String> columns;
    private OrderByRequest orderBy;

    public List<String> getColumns() {
        return columns;
    }

    public void setColumns(List<String> columns) {
        this.columns = columns;
    }

    public OrderByRequest getOrderBy() {
        return orderBy;
    }

    public void setOrderBy(OrderByRequest orderBy) {
        this.orderBy = orderBy;
    }
}
