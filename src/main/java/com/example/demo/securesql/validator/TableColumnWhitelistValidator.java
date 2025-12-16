package com.example.demo.securesql.validator;

import com.example.demo.securesql.parser.SqlMeta;
import com.example.demo.securesql.whitelist.DynamicTableWhitelistRegistry;

import java.util.HashSet;
import java.util.Set;

/**
 * 테이블/컬럼 화이트리스트 검증
 *
 * ✔ rootTables / subQueryTables 모두 검사
 * ✔ alias → 실제 테이블 매핑 지원
 * ✔ SELECT / WHERE / JOIN / GROUP BY / ORDER BY 컬럼 검사
 * ✔ WITH CTE 이름은 "가상 테이블"로 간주하여 검사 제외
 */
public class TableColumnWhitelistValidator implements SqlValidator {

    @Override
    public void validate(SqlMeta meta) {

        // ===============================
        // 1️⃣ CTE 이름 추출
        // ===============================
        // 파서에서 WITH CTE 이름도 meta.getTables()에 들어오므로
        // "화이트리스트에 없지만 실제 테이블도 아닌 이름"을 CTE로 간주
        Set<String> cteNames = extractCteNames(meta);

        // ===============================
        // 2️⃣ 테이블 화이트리스트 검증
        // ===============================
        for (String table : meta.getTables()) {

            // 서브쿼리 가상 테이블 스킵
            if ("__SUBQUERY__".equals(table)) continue;

            // ✅ CTE 이름이면 검사 제외
            if (cteNames.contains(table)) continue;

            // 실제 테이블 화이트리스트 검사
            if (DynamicTableWhitelistRegistry
                    .getColumnsForTable(table).isEmpty()) {
                throw new RuntimeException(
                        "화이트리스트에 등록되지 않은 테이블입니다: " + table
                );
            }
        }

        // ===============================
        // 3️⃣ 컬럼 화이트리스트 검증
        // ===============================
        for (String col : meta.getColumns()) {

            // "*" 또는 "T.*" 스킵
            if ("*".equals(col) || col.endsWith(".*")) continue;

            // prefix 없는 컬럼
            if (!col.contains(".")) {
                if (meta.getRootTables().size() == 1) {
                    String table = meta.getRootTables().iterator().next();

                    // CTE 루트 테이블이면 스킵
                    if (cteNames.contains(table)) continue;

                    if (!DynamicTableWhitelistRegistry
                            .isAllowedColumn(table, col)) {
                        throw new RuntimeException(
                                "허용되지 않은 컬럼입니다: " + table + "." + col
                        );
                    }
                }
                continue;
            }

            // prefix.column 형태
            String[] parts = col.split("\\.");
            String prefix = parts[0];
            String column = parts[1];

            // alias → 실제 테이블 매핑
            String table = meta.getAliasToTable().get(prefix);
            if (table == null) {
                table = prefix;
            }

            // 서브쿼리 컬럼 스킵
            if ("__SUBQUERY__".equals(table)) continue;

            // ✅ CTE 컬럼 스킵
            if (cteNames.contains(table)) continue;

            // 실제 테이블 + 컬럼 화이트리스트 검사
            if (!DynamicTableWhitelistRegistry
                    .isAllowedColumn(table, column)) {
                throw new RuntimeException(
                        "허용되지 않은 컬럼입니다: " + table + "." + column
                );
            }
        }
    }

    /**
     * CTE 이름 추출
     *
     * - 화이트리스트에 없는 테이블 중
     * - aliasToTable 에도 없는 이름을 CTE로 간주
     */
    private Set<String> extractCteNames(SqlMeta meta) {

        Set<String> cteNames = new HashSet<>();

        for (String table : meta.getTables()) {

            if ("__SUBQUERY__".equals(table)) continue;

            // alias가 아니고
            boolean isAlias = meta.getAliasToTable().containsKey(table);

            // 화이트리스트에도 없으면 → CTE
            boolean notInWhitelist =
                    DynamicTableWhitelistRegistry
                            .getColumnsForTable(table).isEmpty();

            if (!isAlias && notInWhitelist) {
                cteNames.add(table);
            }
        }

        return cteNames;
    }
}
