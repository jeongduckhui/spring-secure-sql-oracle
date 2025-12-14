package com.example.demo.report.controller;

import org.springframework.web.bind.annotation.*;

import com.example.demo.report.dto.SalesReportRequest;
import com.example.demo.report.service.SalesReportService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/report")
public class SalesReportController {

    private final SalesReportService service;

    public SalesReportController(SalesReportService service) {
        this.service = service;
    }

    @PostMapping("/sales")
    public List<Map<String, Object>> salesReport(
            @RequestBody SalesReportRequest req
    ) {
        return service.selectSalesReport(req);
    }
}
