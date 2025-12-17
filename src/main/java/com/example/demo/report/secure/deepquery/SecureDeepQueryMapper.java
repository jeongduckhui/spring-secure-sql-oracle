package com.example.demo.report.secure.deepquery;

import java.util.List;
import java.util.Map;

import com.example.demo.securesql.annotation.SecureSqlRequired;

public interface SecureDeepQueryMapper {
	
	@SecureSqlRequired
    List<Map<String, Object>> execute(Map<String, Object> param);
}
