package com.example.demo.securesql.validator;

import com.example.demo.securesql.parser.SqlMeta;

/**
 * OR 정책 Validator
 *
 * - OR 자체는 허용
 * - 상수 비교 OR, 조건 무력화 OR 차단 ('1'='1' 같은 위험 패턴만 차단)
 */
public class OrPolicyValidator implements SqlValidator {

    @Override
    public void validate(SqlMeta meta) {

    	// 'unsafeOR' 플래그는 파싱 단계에서 OR 연산자의 좌우 항 중 하나라도
    	// 컬럼 참조 없이 상수(리터럴)만을 비교하는 패턴('A'='A' 등)이 발견되었을 때 설정
    	// SqlMeta에 'unsafeOR' 플래그가 설정되어 있는지 확인
        if (meta.hasUnsafeOrPredicate()) {
        	// 위험한 패턴이 발견되면 쿼리 실행을 차단
        	// 이 패턴(OR '1'='1')은 SQL Injection(예: 인증 우회)에 직접적으로 사용됨
            throw new RuntimeException(
                "OR 조건에 상수 비교 또는 조건 무력화 패턴이 포함되어 있습니다"
            );
        }
    }
}
