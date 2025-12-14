package com.example.demo.securesql.validator;

import com.example.demo.securesql.parser.SqlMeta;
import com.example.demo.securesql.whitelist.DynamicTableWhitelistRegistry;

/**
 * 테이블/컬럼 화이트리스트 검증
 *
 * - rootTables/subQueryTables 모두 검사
 * - alias -> 실제 테이블 매핑 지원
 * - SELECT/WHERE/JOIN 컬럼 전체 검사
 */
public class TableColumnWhitelistValidator implements SqlValidator {

    @Override
    public void validate(SqlMeta meta) {

        // 테이블 whitelist 검증
    	// meta.getTables(): 파서가 수집한 "쿼리 내 등장 테이블 전체" 목록을 순회
    	// (루트 테이블 + 서브쿼리 처리 시 추가된 테이블 포함)
        for (String table : meta.getTables()) {

            // 서브쿼리 가상 테이블은 검사 제외
        	// 파서에서 서브쿼리를 실제 테이블 대신 "__SUBQUERY__"라는 가상 값으로 넣어둠
        	// 이 값은 화이트리스트 파일에 존재할 수 없고 검사 대상도 아니므로 스킵
            if ("__SUBQUERY__".equals(table)) continue;

            // 화이트리스트 레지스트리에서 해당 테이블의 허용 컬럼 집합을 가져옴
            // 비어있으면 (테이블이 등록되지 않았거나 컬럼 목록이 없으면) 화이트리스트 미등록 테이블로 판단
            if (DynamicTableWhitelistRegistry
                    .getColumnsForTable(table).isEmpty()) {
            	// 테이블이 화이트리스트에 없으면 즉시 차단(예외 발생)
                throw new RuntimeException(
                    "화이트리스트에 등록되지 않은 테이블입니다: " + table
                );
            }
        }

        // 컬럼 whitelist 검증
        // meta.getColumns(): 파서가 수집한 "쿼리에서 참조된 컬럼 전체"를 순회
        // 여기에는 SELECT/WHERE/JOIN/GROUP BY/HAVING/ORDER BY 등에서 collectExpr로 모은 컬럼이 포함됨
        for (String col : meta.getColumns()) {

        	// "*" 또는 "T.*" 형태는 컬럼 화이트리스트 검사에서 제외
        	// (원칙적으로 SelectStarValidator에서 이미 금지하지만, 방어적으로 여기서도 스킵 처리)
            if ("*".equals(col) || col.endsWith(".*")) continue;

            // 컬럼 표현에 '.'이 없으면 prefix 없는 컬럼명 단독 형태. 예: "STORE_ID"
            if (!col.contains(".")) {
            	// 루트 테이블이 1개인 단일 테이블 쿼리일 때만,
            	// 단독 컬럼명을 "그 단일 테이블의 컬럼"으로 간주해서 검사할 수 있음
                if (meta.getRootTables().size() == 1) {
                	// 단일 루트 테이블명을 하나 꺼냄 
                    String table = meta.getRootTables().iterator().next();
                    // 화이트리스트에 (table, col) 조합이 허용되어 있는지 검사
                    // 허용 목록에 없으면 정책 위반
                    if (!DynamicTableWhitelistRegistry
                            .isAllowedColumn(table, col)) {
                    	// 허용되지 않은 컬럼이면 차단(예외 발생)
                        throw new RuntimeException(
                            "허용되지 않은 컬럼입니다: " + table + "." + col
                        );
                    }
                }
                
                // prefix 없는 컬럼 처리를 끝냈으니 다음 컬럼으로 넘어감
                continue;
            }

            // "A.STORE_ID" 같은 형태를 '.' 기준으로 분리
            String[] parts = col.split("\\.");
            // 앞부분(prefix): 보통 alias 또는 테이블명 (예: "A")
            String prefix = parts[0];
            // 뒷부분(column): 실제 컬럼명 (예: "STORE_ID")
            String column = parts[1];

            // prefix가 alias라면 aliasToTable 맵에서 실제 테이블명을 찾아옴. 예: A -> SALES_TRANSACTION
            String table = meta.getAliasToTable().get(prefix);
            
            // aliasToTable에 prefix가 없으면, prefix 자체를 테이블명으로 사용한 것으로 해석
            // 예: SALES_TRANSACTION.STORE_ID 처럼 테이블명을 직접 prefix로 쓴 케이스
            if (table == null) {
            	// prefix를 테이블명으로 대체
                table = prefix;
            }

            // 서브쿼리 컬럼은 검사 제외
            // prefix가 서브쿼리 alias로 매핑되어 "__SUBQUERY__"가 된 경우,
            // 서브쿼리 내부 컬럼은 이 Validator에서 테이블 화이트리스트로 검사할 수 없으므로 스킵
            if ("__SUBQUERY__".equals(table)) continue;

            // 실제 테이블(table)과 컬럼(column) 조합이 화이트리스트에 허용되어 있는지 검사
            if (!DynamicTableWhitelistRegistry
                    .isAllowedColumn(table, column)) {
            	// 허용되지 않은 컬럼이면 차단(예외 발생
                throw new RuntimeException(
                    "허용되지 않은 컬럼입니다: " + table + "." + column
                );
            }
        }
    }
}
