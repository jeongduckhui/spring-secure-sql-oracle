package com.example.demo.securesql.validator;

import com.example.demo.securesql.log.SecureSqlLogger;
import com.example.demo.securesql.parser.OracleAstParser;
import com.example.demo.securesql.parser.SqlMeta;

import java.util.List;

/** Oracle SQL 검증의 중앙 관리 클래스 **/
public class OracleValidator {
	
	/*
	 * ValidatorChain의 검증기 실행순서는 매우 중요
	 * '구조 및 성능 정책 -> 핵심 화이트리스트 보안 -> 특수 보안 패턴 차단' 순서로 구성되어 있음
	 * 순서가 바뀌면 불필요한 리소스 낭비나 보안 허점이 발생할 수 있음
	 * 
	 * - 1. JoinPolicyValidator
	 * 		- 검증 내용: 카테시안 곱 (조건 없는 다중 테이블 조회) 차단
	 * 		- 중요성 및 이유: (정책/성능) WHERE 조건이 없는 쿼리는 데이터베이스에 큰 부하를 줄 수 있으므로, 
	 * 			이 정책 위반은 가장 먼저 빠르게 차단하는 것이 성능상 유리
	 * 
	 * - 2. SelectStarValidator
	 * 		- 검증 내용: SELECT * 또는 T.* 차단
	 * 		- 중요성 및 이유: (정책/보안) 개발 표준이나 민감 정보 노출을 방지하는 기본적인 규칙임
	 * 			간단한 문자열 비교(메타 정보의 * 또는 .* 확인)만으로 빠르게 검증할 수 있음
	 * 
	 * - 3. PrefixRuleValidator
	 * 		- 검증 내용: 다중 테이블 조회 시 컬럼 접두사 사용 강제
	 * 		- 중요성 및 이유: 화이트리스트 검증(4번) 전에 실행하여 SQL의 명확성을 먼저 확보
	 * 
	 * - 4. TableColumnWhitelistValidator
	 * 		- 검증 내용: 테이블 및 컬럼 화이트리스트 검증
	 * 		- 중요성 및 이유: SqlMeta에 수집된 모든 테이블과 모든 컬럼(rootColumns와 columns 모두) 정보를 기반으로 실행
	 * 			앞선 단계에서 구조적 문제(Prefix, Join)가 해결된 후, 누락 없이 검증
	 * 
	 * - 5. FunctionWhitelistValidator
	 * 		- 검증 내용: 함수 화이트리스트 검증
	 * 		- 중요성 및 이유: SqlMeta에 수집된 모든 함수/표현식 정보를 기반으로 실행
	 * 			4번과 함께 핵심 보안 규칙을 담당
	 * 
	 * - 6. OrPolicyValidator
	 * 		- 검증 내용: unsafeOrPredicate (상수 비교 OR) 차단
	 * 		- 중요성 및 이유: (SQL Injection 방어) SQL Injection 시도와 직결되는 패턴(예: OR '1'='1')을 차단
	 * 			이 플래그(unsafeOr)는 파싱 단계에서 설정되지만, 모든 다른 정책 검증을 통과한 후 
	 * 			최종적으로 보안 위험을 확인하여 쿼리를 거부하는 것이 일반적임
	 */

	/** 검증 체인 정의: 모든 개별 Validator를 순서대로 등록 **/
    private static final ValidatorChain CHAIN =
        new ValidatorChain()
        	// [정책] 다중 테이블 조회 시 조건(JOIN/WHERE) 필수 검증
            .add(new JoinPolicyValidator())
            // [정책] SELECT * 또는 TABLE.* 사용 금지 검증
            .add(new SelectStarValidator())
            // [정책] 다중 테이블 조회 시 컬럼 prefix 사용 필수 검증
            .add(new PrefixRuleValidator())
            // [보안] 쿼리 전체 테이블/컬럼이 화이트리스트에 있는지 검증
            .add(new TableColumnWhitelistValidator())
            // [보안] 쿼리 전체 함수/표현식이 화이트리스트에 있는지 검증
            .add(new FunctionWhitelistValidator())
            // [보안] unsafeOr (상수 비교) 패턴만 차단하는 정책 검증기
            .add(new OrPolicyValidator());   

    /** SQL 쿼리를 파싱하고 정의된 검증 체인을 순차적으로 실행하는 메인 검증 메서드 **/
    public static void validate(String sql) {

        try {
        	// [1단계 검증] 가장 빠르고 기본적인 검증: 원시 SQL 문자열에서 금지된 키워드(DDL/시스템 함수) 확인
            ForbiddenKeywordValidator.validateRawSql(sql);

            // [2단계 파싱] JSqlParser를 사용하여 SQL을 파싱하고 메타데이터(SqlMeta) 추출
            List<SqlMeta> metas = new OracleAstParser().parse(sql);

            // 파싱된 모든 SelectBody (복합 쿼리/SetOperation 포함)에 대해 순회하며 검증 실행
            for (SqlMeta meta : metas) {
            	// 추출된 메타데이터(meta)를 ValidatorChain을 통해 순차적으로 검증
                CHAIN.validate(meta);
                // 검증 통과 후, 분석된 메타 정보를 로깅
                SecureSqlLogger.logMeta(meta);
            }

            // 모든 검증을 통과한 경우 성공 로깅
            SecureSqlLogger.logPass(sql);

        } catch (RuntimeException e) {
        	// 검증 중 RuntimeException 발생 시 실패한 SQL과 오류 메시지를 로깅
            SecureSqlLogger.logReject(sql, e.getMessage());
            // 예외를 호출자에게 다시 던져서 SQL 실행을 최종적으로 차단(정상 흐름 차단)
            throw e;
        }
    }
}
