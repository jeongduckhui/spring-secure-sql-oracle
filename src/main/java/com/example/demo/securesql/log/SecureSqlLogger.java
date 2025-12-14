package com.example.demo.securesql.log;

import com.example.demo.securesql.parser.SqlMeta;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SecureSqlLogger {

	/** SQL 검증에 통과했을 때 (PASS) 로그를 출력 **/
    public static void logPass(String sql) {
        log.info("[SECURE-SQL][PASS] {}", sql);
    }

    /** SQL 검증에 실패했을 때 (REJECT) 로그를 출력 **/
    public static void logReject(String sql, String reason) {
        log.info("[SECURE-SQL][REJECT] reason=\"{}\" sql=\"{}\"", reason, sql);
    }

    /** 파싱된 SQL 구문의 메타 정보 (테이블, 칼럼)를 로그로 출력 **/
    public static void logMeta(SqlMeta meta) {
        log.info(
            "[SECURE-SQL][META] tables={} columns={}",
            meta.getRootTables(),
            meta.getRootColumns()
        );
    }
}