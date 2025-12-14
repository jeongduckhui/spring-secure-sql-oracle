package com.example.demo.report.service;

import com.example.demo.report.dto.OrderByRequest;
import com.example.demo.report.dto.SalesReportRequest;
import com.example.demo.report.enums.SalesReportColumn;
import com.example.demo.report.enums.SalesReportOrderBy;
import com.example.demo.report.mapper.SalesReportMapper;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SalesReportService {

    private final SalesReportMapper mapper;

    public SalesReportService(SalesReportMapper mapper) {
        this.mapper = mapper;
    }

    public List<Map<String, Object>> selectSalesReport(
            SalesReportRequest req
    ) {

        /* ======================
           1. SELECT enum 검증
           ====================== */
        List<SalesReportColumn> columns =
                req.getColumns().stream()
                        .map(SalesReportColumn::fromKey)
                        .toList();

        if (columns.isEmpty()) {
            throw new IllegalArgumentException("조회 컬럼이 없습니다");
        }

        /* ======================
           2. SELECT SQL 생성
           ====================== */
        String selectColumns = columns.stream()
                .map(SalesReportColumn::sql)
                .collect(Collectors.joining(", "));

        /* ======================
           3. GROUP BY 자동 생성
           ====================== */
        String groupByColumns = columns.stream()
                .filter(c -> !c.isAggregate())
                .map(SalesReportColumn::sql)
                .collect(Collectors.joining(", "));

        if (groupByColumns.isBlank()) {
            throw new IllegalStateException("GROUP BY 대상 컬럼이 없습니다");
        }

        /* ======================
           4. ORDER BY 처리
           ====================== */
        String orderByClause = buildOrderBy(req.getOrderBy());

        Map<String, Object> param = new HashMap<>();
        param.put("selectColumns", selectColumns);
        param.put("groupByColumns", groupByColumns);
        param.put("orderByClause", orderByClause);

        return mapper.selectSalesReport(param);
    }

    private String buildOrderBy(OrderByRequest req) {

        if (req == null) return "";

        SalesReportOrderBy ob =
                SalesReportOrderBy.fromKey(req.getKey());

        String dir = req.getDir();
        if (!"ASC".equalsIgnoreCase(dir) &&
            !"DESC".equalsIgnoreCase(dir)) {
            throw new IllegalArgumentException("ORDER BY 방향 오류");
        }

        return " ORDER BY " + ob.sql() + " " + dir;
    }
}
