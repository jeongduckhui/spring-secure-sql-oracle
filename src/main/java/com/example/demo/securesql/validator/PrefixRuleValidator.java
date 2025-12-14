package com.example.demo.securesql.validator;

import com.example.demo.securesql.parser.SqlMeta;

/** 컬럼 prefix 규칙(테이블명/별칭.컬럼 형태)을 검증하는 Validator **/
public class PrefixRuleValidator implements SqlValidator {

    @Override
    public void validate(SqlMeta meta) {

    	// 루트 테이블(FROM/JOIN 최상위에 등장한 테이블) 수가 2개 이상이면 다중 테이블 쿼리로 판단
        boolean multiTable = meta.getRootTables().size() > 1;

        // SELECT 절에 직접 등장한 "루트 컬럼 목록"을 하나씩 검사
        // 예: SELECT A.COL, B.COL, COL FROM ...  -> 각각 "A.COL", "B.COL", "COL"
        for (String col : meta.getRootColumns()) {

        	// SELECT * 또는 SELECT T.* 같은 전체 컬럼 조회는 여기서 prefix 규칙 검사 대상이 아님
        	// (별도로 SelectStarValidator에서 이미 금지하고 있고, 여기선 prefix 검사만 책임지므로 스킵)
            if ("*".equals(col) || col.endsWith(".*")) continue;

            // 컬럼 문자열에 '.'이 없으면 prefix(테이블/alias)가 없는 형태
            // 예: "STORE_ID" 처럼 단독 컬럼명만 있는 경우
            if (!col.contains(".")) {
            	// 다중 테이블 쿼리에서는 단독 컬럼명은 어떤 테이블의 컬럼인지 모호해서 허용하지 않음
            	// 또한 공격/실수로 다른 테이블 컬럼을 참조할 위험을 줄이기 위한 정책
                if (multiTable) {
                	// 정책 위반 시 RuntimeException을 던져 상위에서 reject 처리
                    throw new RuntimeException(
                        "다중 테이블 쿼리에서는 prefix 없는 컬럼을 허용하지 않습니다: " + col
                    );
                }
                
                // 단일 테이블 쿼리인 경우(prefix 없는 컬럼) 허용 -> 다음 컬럼 검사로 넘어감
                continue;
            }

            // "A.STORE_ID" 처럼 prefix가 있는 경우 '.' 기준으로 분리하여 첫 번째 토큰을 prefix로 추출
            // prefix는 보통 "테이블명" 또는 "테이블 alias"가 됨
            String prefix = col.split("\\.")[0];

	         // meta에 저장된 aliasToTable 맵에 prefix가 존재하면,
	         // 해당 prefix는 FROM/JOIN에서 선언된 별칭(alias)으로 인정됨(서브쿼리 alias 포함)
	         // 따라서 prefix가 유효하므로 통과
            if (meta.getAliasToTable().containsKey(prefix)) {
            	// 유효한 prefix이므로 다음 컬럼으로 넘어감
                continue;
            }

		    // alias가 아니라도 prefix가 실제 루트 테이블명이라면 유효한 prefix로 인정
		    // 예: FROM SALES_TRANSACTION ... SELECT SALES_TRANSACTION.STORE_ID
            if (meta.getRootTables().contains(prefix)) {
            	// 유효한 prefix이므로 다음 컬럼으로 넘어감
                continue;
            }

            // alias도 아니고, 루트 테이블명도 아닌 prefix라면
            // "정의되지 않은 테이블/별칭"을 컬럼 prefix로 사용한 것이므로 차단
            throw new RuntimeException(
                "컬럼 prefix가 테이블/alias 에 매핑되지 않습니다: " + col
            );
        }
    }
}

