package com.example.demo.report.secure.subquery;

import com.example.demo.report.secure.column.SecureEnterpriseRiskColumn;
import com.example.demo.report.secure.dto.SecureEnterpriseRiskRequest;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SecureSubQueryService {

    private final SecureSubQueryMapper mapper;

    public SecureSubQueryService(SecureSubQueryMapper mapper) {
        this.mapper = mapper;
    }

    public List<Map<String, Object>> execute(SecureEnterpriseRiskRequest req) {

        List<SecureEnterpriseRiskColumn> cols =
                req.getColumns().stream()
                        .map(SecureEnterpriseRiskColumn::from)
                        .toList();

        String selectColumns = cols.stream()
                .map(SecureEnterpriseRiskColumn::selectSql)
                .collect(Collectors.joining(", "));

        String orderByClause = "";
        if (req.getOrderBy() != null) {
            SecureEnterpriseRiskColumn ob =
                    SecureEnterpriseRiskColumn.from(req.getOrderBy().getKey());

            orderByClause =
                    " ORDER BY " + ob.rawSql() +
                    " " + req.getOrderBy().getDir();
        }

        Map<String, Object> param = new HashMap<>();
        param.put("selectColumns", selectColumns);
        param.put("enterpriseIds", req.getEnterpriseIds());
        param.put("orderByClause", orderByClause);

        return mapper.selectEnterpriseRisk(param);
    }
}
