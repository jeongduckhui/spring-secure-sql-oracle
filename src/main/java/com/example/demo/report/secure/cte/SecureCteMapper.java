package com.example.demo.report.secure.cte;

import java.util.List;
import java.util.Map;

import com.example.demo.securesql.annotation.SecureSqlRequired;

public interface SecureCteMapper {
	@SecureSqlRequired
    List<Map<String, Object>> selectEnterpriseRisk(Map<String, Object> param);
}
