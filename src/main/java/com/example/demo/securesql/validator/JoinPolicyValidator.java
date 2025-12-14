package com.example.demo.securesql.validator;

import com.example.demo.securesql.parser.SqlMeta;

/**
 * JOIN 정책 Validator
 *
 * - comma join(FROM A, B)은 WHERE 조건 필수
 * - JOIN ... ON 은 허용
 */
public class JoinPolicyValidator implements SqlValidator {

    @Override
    public void validate(SqlMeta meta) {

    	// rootTables(루트 FROM/JOIN에 등장한 테이블들)의 개수가 2개 이상이면 "다중 테이블 조회"로 판단
        boolean multiTable = meta.getRootTables().size() > 1;

        // 다중 테이블인데 JOIN/WHERE 조건이 하나도 없으면
        // 즉, 조건 없는 다중 테이블 조회(카티전 곱 가능성)가 되므로 차단
        if (multiTable && !meta.hasJoinOrWhereCondition()) {
        	// 정책 위반 시 RuntimeException을 던져 상위 OracleValidator에서 reject 처리되게 함
            throw new RuntimeException(
                "다중 테이블 조회 시 JOIN 또는 WHERE 조건이 필요합니다 (Cartesian Product 차단)"
            );
        }
    }
}
