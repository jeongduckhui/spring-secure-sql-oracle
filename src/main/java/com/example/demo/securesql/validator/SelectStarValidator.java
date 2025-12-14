package com.example.demo.securesql.validator;

import com.example.demo.securesql.parser.SqlMeta;

/**
 * SELECT * 정책
 *
 * 	- 단일/다중 테이블 상관없이 SELECT * 금지
 * 	- SELECT T.* 도 금지
 *  
 *  - 전체 컬럼 조회(*)는 과다 조회/권한 우회/성능 문제를 유발할 수 있어 금지
 */
public class SelectStarValidator implements SqlValidator {

    @Override
    public void validate(SqlMeta meta) {

    	// SELECT 절의 "루트 컬럼 목록"을 순회
    	// rootColumns는 SELECT 리스트에 직접 등장한 컬럼/표현식(또는 * / T.*)을 수집해둔 값
    	// 예: SELECT A.COL, B.COL FROM ...  → "A.COL", "B.COL"
		//      SELECT * FROM ...            → "*"
		//      SELECT A.* FROM ...          → "A.*"
        for (String c : meta.getRootColumns()) {
        	// 현재 컬럼 문자열이 정확히 "*" 이면 SELECT * 패턴
        	// "무엇이든 + .*" 로 끝나면 SELECT T.* (테이블명/alias 전체 컬럼) 패턴
        	// 둘 중 하나라도 해당하면 정책 위반으로 판단
            if ("*".equals(c) || c.endsWith(".*")) {
            	// 정책 위반 시 RuntimeException 발생 → 상위 OracleValidator에서 reject 처리
                throw new RuntimeException("SELECT * (또는 TABLE.*) 는 허용되지 않습니다");
            }
        }
    }
}
