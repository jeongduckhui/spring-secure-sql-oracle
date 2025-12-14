package com.example.demo.securesql.validator;

import com.example.demo.securesql.parser.SqlMeta;

/**
 * SqlMeta 객체에 저장된 메타 정보를 기반으로 SQL 쿼리의 보안 및 정책을 검증.
 * 검증에 실패하면 RuntimeException을 발생시켜 쿼리 실행을 차단합.
 *
 * @param meta OracleAstParser를 통해 파싱된 SQL 쿼리의 메타 정보
 */
public interface SqlValidator {
	// 검증 로직을 수행하는 유일한 메서드 정의 (모든 Validator가 이 메서드를 구현해야 함)
    void validate(SqlMeta meta);
}
