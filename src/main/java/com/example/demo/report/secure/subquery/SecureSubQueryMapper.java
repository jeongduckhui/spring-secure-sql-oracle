package com.example.demo.report.secure.subquery;

import java.util.List;
import java.util.Map;

public interface SecureSubQueryMapper {
    List<Map<String, Object>> selectEnterpriseRisk(Map<String, Object> param);
}
