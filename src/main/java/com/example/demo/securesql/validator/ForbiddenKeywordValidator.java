package com.example.demo.securesql.validator;

import java.util.Locale;
import java.util.regex.Pattern;

public class ForbiddenKeywordValidator {

    /**
     * 금지 키워드 (SQL 토큰 기준)
     * DDL, DML 명령 및 위험한 Oracle 시스템/PL/SQL 호출을 포함
     */
    private static final String[] FORBIDDEN = {
    	// 테이블/데이터베이스 삭제
        "DROP",
        // 테이블 내용 빠른 삭제
        "TRUNCATE",
        // 데이터 삭제 (DML)
        "DELETE",
        // 테이블 구조 변경 (DDL)
        "ALTER",
        // 이름 변경 (DDL)
        "RENAME",
        // 데이터 병합 (DML)
        "MERGE",
        // 권한 부여 (DCL)
        "GRANT",
        // 권한 회수 (DCL)
        "REVOKE",
        // Oracle PL/SQL 동적 SQL 실행
        "EXECUTE\\s+IMMEDIATE",
        // Oracle 파일 시스템 접근 패키지
        "UTL_FILE",
        // Oracle 동적 SQL 실행 패키지
        "DBMS_SQL",
        // Oracle 스케줄러 관리 패키지
        "DBMS_SCHEDULER"
    };

    // 컴파일된 정규 표현식 패턴을 저장할 배열
    private static final Pattern[] PATTERNS;

    // 클래스 로딩 시 단 한 번 실행되는 정적 초기화 블록
    static {
    	// 금지 키워드 수만큼 배열 크기 할당
        PATTERNS = new Pattern[FORBIDDEN.length];
        for (int i = 0; i < FORBIDDEN.length; i++) {
            // 단어 경계 기반 매칭을 위한 패턴 생성
            // 예: DROP → \bDROP\b. 이는 'DROPSHIP' 같은 컬럼 이름에서 'DROP'이 오탐되는 것을 방지
            PATTERNS[i] = Pattern.compile(
            	// 단어 경계 \b와 키워드를 결합
                "\\b" + FORBIDDEN[i] + "\\b",
                // 대소문자 구분 없이 매칭 (SQL은 대소문자 구분을 하지 않으므로)
                Pattern.CASE_INSENSITIVE
            );
        }
    }

    /**
     * 원시 SQL 문자열에 금지된 키워드가 포함되어 있는지 검증.
     * 이 검증은 파싱보다 먼저 실행되는 1차 방어벽.
     *
     * @param sql 검증할 원본 SQL 문자열
     */
    public static void validateRawSql(String sql) {

    	// null/공백 SQL은 보안 위협이 아니기 때문에 ForbiddenKeywordValidator에서는 통과
        if (sql == null || sql.isBlank()) {
            return;
        }
        
        /*
         * 문자열 리터럴을 제거하는 이유
         * 	- 1. 오탐 방지
         * 		- SELECT 'DROP' AS keyword FROM dual => SELECT '' AS keyword FROM dual
         * 		- 위 쿼리와 같이 문자열을 제거
         * 		- 'DROP'은 DB가 값(value)으로 취급, SQL 키워드로 해석되지 않음.
         * 		- 제거하지 않는다면 upper.contains("DROP")과 같은 검증로직에 체크되어 오탐하게 됨.
         * 		- 문자열 안에 있는 DROP은 DROP이 아님
         * - 2. Injection 안정성 높임
         * 		- SQL Injection의 전형적인 패턴: SELECT * FROM users WHERE name = 'x' OR '1'='1'
         * 		- 문자열 제거: SELECT * FROM users WHERE name = '' OR ''='' 
         * 		- 문자열 제거 후 구조 들어남
         * 		- OrPolicyValidator / isConstantComparison 에 정확히 걸림
         * 		- 문자열 리터럴 제거는 값을 제거해서 구조만 남기는 과정
         */
        // 문자열 리터럴 제거 (alias/컬럼 오탐 방지 + injection 안정성 높임)
        String normalized = stripStringLiterals(sql)
        		// 전체 문자열을 대문자로 변환하여 비교 (SQL의 대소문자 무시 속성 반영)
                .toUpperCase(Locale.ROOT);

        // 모든 금지된 키워드 패턴에 대해 반복
        for (int i = 0; i < PATTERNS.length; i++) {
        	// 정규 표현식 매칭 시도
            if (PATTERNS[i].matcher(normalized).find()) {
            	// 매칭되는 키워드가 발견되면 RuntimeException 발생 및 쿼리 실행 차단
                throw new RuntimeException(
                    "금지된 키워드가 포함되어 있습니다: " +
                    // 사용자에게 보여줄 메시지 포맷팅 (EXECUTE\s+IMMEDIATE 같은 정규식 이스케이프 제거)
                    FORBIDDEN[i].replace("\\s+", " ")
                );
            }
        }
    }

    /**
     * 문자열 리터럴 제거
     * 싱글 쿼트(')로 둘러싸인 모든 내용을 빈 문자열 리터럴(')로 대체합니다.
     * 예: WHERE col = 'USER_DROP_A' -> WHERE col = ''
     */
    private static String stripStringLiterals(String sql) {
    	// 정규식: '([^']|'')*'
        // ' 로 시작하여, 다음 중 하나가 반복됨: (싱글 쿼트가 아닌 모든 문자) 또는 ('' 이스케이프된 싱글 쿼트)
        // 닫는 ' 가 나오면 매칭 종료
        return sql.replaceAll("'([^']|'')*'", "''");
    }
}
