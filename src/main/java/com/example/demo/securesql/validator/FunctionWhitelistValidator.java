package com.example.demo.securesql.validator;

import com.example.demo.securesql.parser.SqlMeta;
import com.example.demo.securesql.whitelist.GlobalFunctionWhitelistRegistry;

/**
 * 함수 화이트리스트 Validator
 *
 * - NVL, DECODE, SUM, COUNT, CASE 등
 * - FunctionWhitelist.properties 기반
 */
public class FunctionWhitelistValidator implements SqlValidator {

    @Override
    public void validate(SqlMeta meta) {

    	// SqlMeta에 파싱되어 수집된 모든 함수 및 표현식 목록을 순회
        for (String func : meta.getExpressions()) {

        	// 현재 함수(func)가 GlobalFunctionWhitelistRegistry에 의해 허용되는지 확인
            if (!GlobalFunctionWhitelistRegistry.isAllowedFunction(func)) {
            	// 허용되지 않은 함수인 경우, RuntimeException을 발생시켜 쿼리 실행 차단
                throw new RuntimeException(
                    "허용되지 않은 함수입니다: " + func
                );
            }
        }
    }
}
