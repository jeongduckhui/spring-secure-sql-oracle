package com.example.demo.securesql.parser;

import java.util.*;

/* 
 * OracleAstParser가 SQL 쿼리를 파싱한 후, 
 * 검증(Validation)에 필요한 모든 메타 정보와 보안 위험 플래그를 저장하는 
 * 데이터 컨테이너(Data Transfer Object) 역할 
 */
public class SqlMeta {
	
	/*
	 * 테이블 구분 (rootTables vs. tables)
	 * 
	 *	- rootTables (최상위 테이블)
	 *		- 포함대상: 최상위 FROM 절과 JOIN 절에 직접 나타나는 테이블만 포함 (CTE나 서브쿼리 내부 테이블 제외).
	 *		- 주요 용도: 
	 *			- 메인 쿼리 권한 검증: 이 쿼리가 어떤 주요 데이터 소스에 대해 실행되는지 빠르게 확인
	 *			- 서브쿼리 식별: tables 목록에만 있고 rootTables에 없는 테이블이 있다면, 
	 *				이는 CTE나 서브쿼리 내부에 숨겨진 테이블이며, 쿼리 복잡성(__SUBQUERY__ 마커로 추가 확인) 증가의 지표가 됨
	 *
	 *	- tables (전체 테이블)
	 *		- 포함대상: 쿼리 전체(최상위 쿼리, CTE, 모든 서브쿼리)에 사용된 모든 테이블과 __SUBQUERY__ 마커 포함.
	 *		- 주요 용도:
	 *			- 전체 스키마 검증: 사용자가 접근하려는 모든 테이블(서브쿼리 내부 포함)이 접근 허용 목록(Whitelist)에 포함되는지 검증
	 *			- 데이터 흐름 추적: 쿼리가 데이터를 가져오는 모든 엔티티를 파악
	 * 
	 * 
	 * 칼럼 구분 (rootColumns vs. columns)
	 * 
	 * 	- rootColumns (SELECT 절 칼럼)
	 * 		- 포함대상: 최종 결과 집합(SELECT 리스트)에 포함되는 칼럼 (SELECT A, B, *)만 포함.
	 * 		- 주요 용도: 
	 * 			- 출력 스키마 검증: 사용자에게 노출되는 최종 데이터에 민감 정보(PII 등) 칼럼이 포함되는지 검사 (SELECT 절에 특정 민감 칼럼 사용 금지)
	 * 			- SELECT * 방지: rootColumns에 *가 포함되어 있으면, 불필요하게 많은 데이터를 전송하거나 민감 정보 노출 위험이 있으므로 경고 또는 차단 기준으로 사용
	 * 
	 * 	- columns (전체 사용 칼럼)
	 * 		- 포함대상: 쿼리 전체(SELECT, WHERE, JOIN ON, GROUP BY, HAVING, 함수 인자 등)에 사용된 모든 칼럼 포함.
	 * 		- 주요 용도:
	 * 			- 조건부 권한 검증: WHERE 절에 특정 필터링 칼럼(user_id, tenant_id)의 사용이 강제되는지 확인
	 * 			- 인덱스 효율 분석: 사용된 모든 칼럼을 기반으로 데이터베이스 관리자가 성능 문제를 일으키는 칼럼(예: 인덱스 없는 WHERE 절 칼럼)을 식별하는 데 도움을 줌
	 */

    /* =========================
       테이블 / 컬럼 / 표현식 정보
       ========================= */
	// [테이블] FROM 절의 최상위/직접 테이블 목록 (서브쿼리 내부 테이블 제외)
    private final Set<String> rootTables = new HashSet<>();
    // [테이블] 쿼리 전체(서브쿼리 포함)에 사용된 모든 테이블 목록
    private final Set<String> tables = new HashSet<>();
    // [칼럼] SELECT 절에 직접 명시된 칼럼 목록 (예: SELECT A, B)
    private final Set<String> rootColumns = new HashSet<>();
    // [칼럼] 쿼리 전체(SELECT, WHERE, JOIN 등)에 사용된 모든 칼럼 목록
    private final Set<String> columns = new HashSet<>();
    // [함수] 쿼리에 사용된 함수/표현식 목록 (예: SUM, COUNT, DATE_TRUNC)
    private final Set<String> expressions = new HashSet<>();
    // [별칭] 별칭(Alias)과 실제 테이블 이름의 매핑 (예: "C" -> "CUSTOMERS")
    private final Map<String, String> aliasToTable = new HashMap<>();

    /* =========================
       보안 / 조건 플래그
       ========================= */
    // [보안] OR 연산자 존재 여부 (일반적인 위험 마킹)
    private boolean dangerousOr = false;
    // [보안] OR 연산자 중 Injection 위험이 있는 패턴 존재 여부 (예: '1'='1' 같은 상수 비교)
    private boolean unsafeOr = false;
    // [조건] WHERE, JOIN ON, HAVING 등 조건절 존재 여부
    private boolean hasCondition = false;

    // [조건] WHERE 1=1 (숫자 상수 TRUE) 정상 통과
    private boolean constantTrueInWhere = false;
    // [조건] JOIN ON 상수 비교 (ON 1=1, ON '1'='1') 차단
    private boolean constantComparisonInJoin = false;

    /* =========================
       adders (Parser 전용)
       ========================= */
    // 최상위 테이블 추가 (대문자 변환 후 저장)
    public void addRootTable(String t) { rootTables.add(t.toUpperCase()); }
    // 전체 테이블 목록에 추가 (대문자 변환 후 저장)
    public void addTable(String t) { tables.add(t.toUpperCase()); }
    // SELECT 절의 칼럼 추가 (대문자 변환 후 저장)
    public void addRootColumn(String c) { rootColumns.add(c.toUpperCase()); }
    // 전체 사용 칼럼 목록에 추가 (대문자 변환 후 저장)
    public void addColumn(String c) { columns.add(c.toUpperCase()); }
    // 함수/표현식 목록에 추가 (대문자 변환 후 저장)
    public void addExpression(String e) { expressions.add(e.toUpperCase()); }

    // 별칭과 실제 테이블 이름(또는 __SUBQUERY__) 매핑 추가
    public void addAlias(String a, String t) {
        aliasToTable.put(a.toUpperCase(), t.toUpperCase());
    }

    // OR 연산자 발견 시 호출되어 플래그 설정
    public void markDangerousOr() { dangerousOr = true; }
    // 상수 비교가 포함된 위험한 OR 패턴 발견 시 호출되어 플래그 설정
    public void markUnsafeOr() { unsafeOr = true; }
    // 조건절 (WHERE, JOIN ON, HAVING 등) 발견 시 호출되어 플래그 설정
    public void markCondition() { hasCondition = true; }

    // WHERE 1=1 (숫자 상수 TRUE) 플래그 설정
    public void markConstantTrueInWhere() { constantTrueInWhere = true; }
    // JOIN ON 상수 비교 (ON 1=1, ON '1'='1') 플래그 설정
    public void markConstantComparisonInJoin() { constantComparisonInJoin = true; }

    /* =========================
       getters (Validator 전용)
       ========================= */
    // 최상위 테이블 목록 반환
    public Set<String> getRootTables() { return rootTables; }
    // 전체 사용 테이블 목록 반환
    public Set<String> getTables() { return tables; }
    // SELECT 절 칼럼 목록 반환
    public Set<String> getRootColumns() { return rootColumns; }
    // 전체 사용 칼럼 목록 반환
    public Set<String> getColumns() { return columns; }
    // 사용된 함수/표현식 목록 반환
    public Set<String> getExpressions() { return expressions; }
    // 별칭 맵 반환
    public Map<String, String> getAliasToTable() { return aliasToTable; }

    // 일반 OR 연산자 존재 여부 반환 (권한 모델 검증에 사용될 수 있음)
    public boolean hasDangerousOrPredicate() { return dangerousOr; }
    // Injection 위험 OR 패턴 존재 여부 반환 (가장 엄격한 차단 기준)
    public boolean hasUnsafeOrPredicate() { return unsafeOr; }
    // 조건절 존재 여부 반환 (특정 권한 없는 사용자의 필터링 없는 전수 조회 차단 등에 사용될 수 있음)
    public boolean hasJoinOrWhereCondition() { return hasCondition; }

    // WHERE 1=1 (숫자 상수 TRUE) 패턴 존재 여부 반환
    public boolean hasConstantTrueInWhere() { return constantTrueInWhere; }
    // JOIN ON 상수 비교 (ON 1=1, ON '1'='1') 패턴 존재 여부 반환
    public boolean hasConstantComparisonInJoin() { return constantComparisonInJoin; }
}
