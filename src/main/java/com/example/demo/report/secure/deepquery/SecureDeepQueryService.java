package com.example.demo.report.secure.deepquery;

import com.example.demo.report.secure.column.SecureDeepQueryColumn;
import com.example.demo.report.secure.dto.SecureDeepQueryRequest;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SecureDeepQueryService {

    private final SecureDeepQueryMapper mapper;

    public SecureDeepQueryService(SecureDeepQueryMapper mapper) {
        this.mapper = mapper;
    }

    public List<Map<String, Object>> execute(SecureDeepQueryRequest req) {

        String selectColumns = req.getSelectColumns().stream()
                .map(SecureDeepQueryColumn::from)
                .map(SecureDeepQueryColumn::selectSql)
                .collect(Collectors.joining(", "));

        String groupByColumns = req.getGroupByColumns().stream()
                .map(SecureDeepQueryColumn::from)
                .map(SecureDeepQueryColumn::rawSql)
                .collect(Collectors.joining(", "));

        String orderByColumns = req.getOrderByColumns().stream()
                .map(SecureDeepQueryColumn::from)
                .map(c -> c.rawSql() + " " + req.getOrderByDir())
                .collect(Collectors.joining(", "));

        // ✅ NPE 방지 (중요)
        String inConditionColumns = Optional.ofNullable(req.getInConditionColumns())
                .orElse(Collections.emptyList())
                .stream()
                .map(SecureDeepQueryColumn::from)
                .map(SecureDeepQueryColumn::rawSql)
                .collect(Collectors.joining(", "));

        Map<String, Object> param = new HashMap<>();
        param.put("selectColumns", selectColumns);
        param.put("groupByColumns", groupByColumns);
        param.put("orderByColumns", orderByColumns);
        param.put("inConditionColumns", inConditionColumns);

        param.put("enterpriseIds", req.getEnterpriseIds());
        param.put("minRevenue", req.getMinRevenue());

        return mapper.execute(param);
    }
}
