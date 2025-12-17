package com.example.demo.securesql.validator;

import com.example.demo.securesql.parser.SqlMeta;

/**
 * OR 정책 Validator
 *
 * 정책:
 * - WHERE 1=1 : 허용
 * - OR + 상수 비교 : 차단
 * - JOIN ON 상수 비교 : 차단
 */
public class OrPolicyValidator implements SqlValidator {

    @Override
    public void validate(SqlMeta meta) {

        // ❌ JOIN ON 1=1 또는 ON '1'='1'
        if (meta.hasConstantComparisonInJoin()) {
            throw new RuntimeException(
                "JOIN 조건에서 상수 비교는 허용되지 않습니다"
            );
        }

        // ❌ OR '1'='1', OR 1=1
        if (meta.hasUnsafeOrPredicate()) {
            throw new RuntimeException(
                "OR 조건에 상수 비교 또는 조건 무력화 패턴이 포함되어 있습니다"
            );
        }

        // ✅ WHERE 1=1 은 허용
    }
}
