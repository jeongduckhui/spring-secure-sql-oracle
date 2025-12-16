package com.example.demo.report.secure.cte;

import java.util.List;
import java.util.Map;

public interface SecureCteMapper {
    List<Map<String, Object>> selectEnterpriseRisk(Map<String, Object> param);
}
