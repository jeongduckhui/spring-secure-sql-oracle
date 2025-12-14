package com.example.demo.securesql.validator;

import com.example.demo.securesql.parser.SqlMeta;

/*
 * OrPolicyValidator로 대체되어 현재는 사용하지 않음
 * 사용하지 않는 이유
 * 	- SQL 쿼리 내에 단순히 OR 연산자가 존재하기만 해도 차단
 *  
 *  사용할 일은 없겠지만 파일을 삭제하지는 않았음 
 */
public class OrInjectionValidator implements SqlValidator {

    @Override
    public void validate(SqlMeta meta) {
    	// OR 연산자에 DangerousOr 플래그를 체크
    	// OR 연산자가 존재하기만 해도 차단
        if (meta.hasDangerousOrPredicate()) {
            throw new RuntimeException(
                "OR 조건으로 컬럼 조건이 무력화됩니다 (SQL Injection 의심)"
            );
        }
    }
}
