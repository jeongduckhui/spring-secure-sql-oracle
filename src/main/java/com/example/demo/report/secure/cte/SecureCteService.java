package com.example.demo.report.secure.cte;

import com.example.demo.report.secure.column.SecureEnterpriseRiskColumn;
import com.example.demo.report.secure.dto.SecureEnterpriseRiskRequest;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SecureCteService {

    private final SecureCteMapper mapper;

    public SecureCteService(SecureCteMapper mapper) {
        this.mapper = mapper;
    }

    public List<Map<String, Object>> execute(SecureEnterpriseRiskRequest req) {

        // 1️⃣ 요청 컬럼 → enum 화이트리스트 검증
        List<SecureEnterpriseRiskColumn> cols =
                req.getColumns().stream()
                        .map(SecureEnterpriseRiskColumn::from)
                        .toList();

        // 2️⃣ SELECT ${selectColumns}
        String selectColumns = cols.stream()
                .map(SecureEnterpriseRiskColumn::selectSql)
                .collect(Collectors.joining(", "));

        // 3️⃣ ORDER BY ${orderByClause}
        String orderByClause = "";
        if (req.getOrderBy() != null) {

            SecureEnterpriseRiskColumn ob =
                    SecureEnterpriseRiskColumn.from(req.getOrderBy().getKey());

            orderByClause =
                    " ORDER BY " + ob.rawSql() +
                    " " + req.getOrderBy().getDir();
        }

        // 4️⃣ Mapper 파라미터
        Map<String, Object> param = new HashMap<>();
        param.put("selectColumns", selectColumns);
        param.put("enterpriseIds", req.getEnterpriseIds());
        param.put("orderByClause", orderByClause);

        return mapper.selectEnterpriseRisk(param);
    }
}
