package com.example.demo.report.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.example.demo.securesql.annotation.SecureSqlRequired;

import java.util.List;
import java.util.Map;

@Mapper
public interface SalesReportMapper {

	@SecureSqlRequired
    List<Map<String, Object>> selectSalesReport(
            Map<String, Object> param
    );
}
