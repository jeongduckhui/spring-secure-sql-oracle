package com.example.demo.securesql.parser;

import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.ExistsExpression;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.*;

import java.util.ArrayList;
import java.util.List;

/**
 * OracleAstParser
 *
 * - SELECT 전용 파서
 * - 구조 수집만 수행 (검증은 Validator 책임)
 */
public class OracleAstParser {

	/*
	 * - net.sf.jsqlparser.statement.Statement (최상위 구문)
	 * 		- SQL 구문의 최상위 추상화 구문
	 * 		- 단일 SQL 문자열(예: INSERT, UPDATE, DELETE, SELECT, CREATE TABLE)을 파싱했을 때 나오는 가장 큰 컨테이너
	 * 		- 역할: 파서(예: CCJSqlParserUtil.parse(sql))가 SQL 텍스트를 분석하여 생성하는 루트 객체
	 *			이는 해당 SQL이 어떤 종류의 명령인지 구분하는 출발점 
	 * 		- 주요 하위 클래스
	 * 			- Select: 데이터를 조회하는 구문.
	 * 			- Insert: 데이터를 삽입하는 구문.
	 * 			- Update: 데이터를 수정하는 구문.
	 * 			- Delete: 데이터를 삭제하는 구문.
	 * 			- CreateView, CreateTable, DropTable 등 DDL 구문.
	 * 
	 * - net.sf.jsqlparser.statement.select.Select (조회 구문)
	 * 		- Statement의 한 종류로서, 데이터를 조회하는 구문(SELECT)을 구체적으로 나타내는 객체
	 * 		- Statement 객체가 SELECT 유형으로 확인되면, 이를 Select 타입으로 안전하게 캐스팅하여 사용
	 * 		- 역할: SELECT 구문 자체의 구조를 담고 있으며, SQL 조회 구문의 핵심인 **SelectBody**를 포함
	 * 		- 주요 속성:
	 * 			- SelectBody getSelectBody(): SELECT 구문의 본문을 반환. 본문은 실제 FROM, WHERE 등의 절을 담음
	 * 			- OrderByElement getOrderBy(): 전체 쿼리의 최종 정렬(ORDER BY) 절을 담음.
	 * 			- Limit getLimit(): 최종 결과에 대한 제한(LIMIT/FETCH FIRST)을 담ㅇ,ㅁ
	 */
	/** SQL 문자열을 파싱하여 쿼리 메타정보를 수집 **/
    public List<SqlMeta> parse(String sql) {

        try {
        	// SQL 문자열을 JSqlParser의 AST(Abstract Syntax Tree) 객체로 파싱
            Statement stmt = CCJSqlParserUtil.parse(sql);

            // 파싱된 Statement가 SELECT 구문이 아니면 빈 리스트 반환
            if (!(stmt instanceof Select)) {
                return List.of();
            }

            // Statement를 Select 타입으로 캐스팅
            Select select = (Select) stmt;
            // 파싱 결과를 담을 SqlMeta 객체 리스트 초기화
            List<SqlMeta> metas = new ArrayList<>();

            // SELECT 문의 본문(SelectBody) 처리 시작 (PlainSelect 또는 SetOperationList)
            handleSelectBody(select.getSelectBody(), metas);

            // 수집된 메타 정보 리스트 반환
            return metas;

        } catch (Exception e) {
        	// 파싱 과정 중 발생한 예외 처리 및 런타임 예외로 변환하여 던짐
            throw new RuntimeException("SQL 파싱 실패: " + e.getMessage(), e);
        }
    }

    /*
     * [ PlainSelect vs. SetOperationList 차이점 비교 ]
     * 
     * JSqlParser에서 PlainSelect와 SetOperationList는 SQL SELECT 쿼리 본문(SelectBody)을 구성하는 
     * 가장 근본적인 두 가지 방식이며, 이들은 쿼리의 구조적 차이를 나타냄
     * 
     * 	- PlainSelect
     * 		- SQL 형태: 단일 SELECT 블록
     * 		- 키워드: 없음 (가장 기본적인 SELECT... FROM... WHERE...)
     * 		- 구조: 하나의 FROM 절과 해당 절에 연결된 JOIN, WHERE, GROUP BY, HAVING 등으로 구성됨.
     * 		- 사용 예시: SELECT * FROM employees WHERE dept = 10
     * 		- 파서 코드 역할: handlePlainSelect 메서드에서 이 단일 블록의 모든 구성 요소 (테이블, 칼럼, 조건 등)를 수집.
     * 
     * 	- SetOperationList
     * 		- SQL 형태: 여러 SELECT 블록의 결합
     * 		- 키워드: UNION, INTERSECT, EXCEPT (또는 MINUS)
     * 		- 구조: 여러 개의 SelectBody 리스트를 포함하며, 각 요소는 PlainSelect 또는 다른 SetOperationList일 수 있음.
     * 		- 사용 예시: SELECT name FROM employees UNION ALL SELECT name FROM contractors
     * 		- 파서 코드 역할: handleSelectBody 메서드에서 하위 SelectBody들을 재귀적으로 순회하며 처리하도록 분기함.
     */
    /** SelectBody 처리 **/
    private void handleSelectBody(SelectBody body, List<SqlMeta> metas) {
    	
    	// SelectBody가 일반적인 SELECT 구문(PlainSelect)인 경우 처리
        if (body instanceof PlainSelect) {
            handlePlainSelect((PlainSelect) body, metas);
        } 
        // SelectBody가 UNION, INTERSECT 등 집합 연산(SetOperationList)인 경우 처리
        else if (body instanceof SetOperationList) {
            SetOperationList sol = (SetOperationList) body;
            // 집합 연산 리스트 내의 각 SelectBody(좌항, 우항)를 재귀적으로 처리
            for (SelectBody sb : sol.getSelects()) {
                handleSelectBody(sb, metas);
            }
            
            // ORDER BY는 개별 SQL에 대한 정렬, 최종 결과 집합에 대한 정렬 
            // 표현식 수집 위치가 2곳임
            
            // 아래에 있는 JSqlParser 4.6의 AST 구조를 보면 ORDER BY 수집 구간이 2곳인 걸 확인할 수 있음
            /*
             * (SELECT col1, COUNT(*) FROM tableA GROUP BY col1)
			 *	UNION
			 * (SELECT col1, COUNT(*) FROM tableB GROUP BY col1)
			 *  ORDER BY 1;
             * 
             *  아래 ORDER BY Expresstion 수집 로직은 위 예시 쿼리에 있는 
             *  ORDER BY 절처럼 UNION, INTERSECT 등 집합 연산의 최종 결과 집합에 대한 정렬을 의미
             */
            // UNION 전체 ORDER BY 처리 (JSqlParser 4.6)
            if (sol.getOrderByElements() != null) {
                for (SqlMeta meta : metas) {
                    for (OrderByElement obe : sol.getOrderByElements()) {
                        if (obe.getExpression() != null) {
                            collectExpr(obe.getExpression(), meta);
                        }
                    }
                }
            }
            
        }
    }
    
    /*
     * [ AllColumns vs. AllTableColumns 차이점 비교 ]
     * 
     * AllColumns와 AllTableColumns는 SQL의 SELECT * 구문을 파싱할 때 사용되는 클래스로, 
     * 둘 다 모든 칼럼을 의미하지만 적용 범위에 명확한 차이가 있음
     * 
     * - AllColumns
     * 		- SQL 표현: *
     * 		- 적용범위: 현재 FROM 절과 JOIN 절에 참여하는 모든 테이블의 모든 컬럼
     * 		- 사용 예시: SELECT * FROM TableA JOIN TableB
     * 		- Parser 코드: handlePlainSelect의 if (item instanceof AllCollumns)에서 처리됨
     * 
     * - AllTableColumns
     *  	- SQL 표현: alias.* 또는 table_name.*
     * 		- 적용범위: 지정된 특정 테이블의 모든 칼럼
     * 		- 사용 예시: SELECT A.*, B.column FROM TableA A JOIN TableB B
     * 		- Parser 코드: handlePlainSelect의 if (item instanceof AllTableColumns) 에서 처리됨
     */
    /** PlainSelect(일반적인 SELECT 구문) 처리 **/
    private void handlePlainSelect(PlainSelect ps, List<SqlMeta> metas) {

    	// 현재 PlainSelect 구문의 메타 정보를 담을 새 SqlMeta 객체 생성
        SqlMeta meta = new SqlMeta();
        metas.add(meta);

        /* ---------- SELECT 절 (칼럼) ---------- */
        // SELECT 절의 각 항목(SelectItem) 순회
        for (SelectItem item : ps.getSelectItems()) {

        	// SELECT * 인 경우: 루트 칼럼에 "*" 마킹
            if (item instanceof AllColumns) {
                meta.addRootColumn("*");
                continue;
            }

            // SELECT table.* 인 경우: 해당 테이블의 모든 칼럼 마킹
            if (item instanceof AllTableColumns) {
                meta.addRootColumn(item.toString());
                continue;
            }

            // SELECT expression [AS alias] 인 경우
            if (item instanceof SelectExpressionItem) {
                Expression expr = ((SelectExpressionItem) item).getExpression();
                // SELECT 항목 내부의 Expression(함수, 연산 등) 수집
                collectExpr(expr, meta);

                // 만약 Expression 자체가 컬럼이라면 루트 컬럼으로 추가
                if (expr instanceof Column) {
                    meta.addRootColumn(expr.toString());
                }
            }
        }
        
        /*
         * JSqlParser 4.6의 AST 구조
         * 	- GROUP BY, HAVING, ORDER BY 절이 SELECT 하위에 있는게 아니라 PlainSelect, SetOperationList 하위에 있음
         *  - ORDER BY 표현식 수집위치가 2곳임
         * 
         * Statement
		 *	 └─ Select
		 *	     └─ SelectBody
		 *	         ├─ PlainSelect
		 *	         │   ├─ SELECT
		 *	         │   ├─ FROM
		 *	         │   ├─ WHERE
		 *	         │   ├─ GROUP BY
		 *	         │   ├─ HAVING
		 *	         │   └─ ORDER BY       
		 *	         │
		 *	         └─ SetOperationList
		 *	             ├─ SelectBody
		 *	             └─ ORDER BY       
         * 
         */
        
        /* ---------- FROM 절 (주 테이블) ---------- */
        // FROM 절의 FromItem(테이블 또는 서브쿼리) 처리
        handleFromItem(ps.getFromItem(), meta);

        /* ---------- JOIN 절 (조인 테이블 및 조건) ---------- */
        if (ps.getJoins() != null) {
            for (Join j : ps.getJoins()) {
            	// JOIN의 오른쪽 FromItem 처리 (JOIN 대상 테이블/서브쿼리)
                handleFromItem(j.getRightItem(), meta);

                // JOIN의 ON 조건 처리
                @SuppressWarnings("unchecked")
                List<Expression> onExprs = (List<Expression>) j.getOnExpressions();
                if (onExprs != null && !onExprs.isEmpty()) {
                	// 조건이 있음을 마킹
                    meta.markCondition();
                    // ON 조건 Expression 수집
                    for (Expression on : onExprs) {
                        collectExpr(on, meta);
                    }
                }
            }
        }

        /* ---------- WHERE 절 ---------- */
        // WHERE 조건이 있는 경우
        if (ps.getWhere() != null) {
        	// 조건이 있음을 마킹
            meta.markCondition();
            // WHERE 조건 Expression 수집
            collectExpr(ps.getWhere(), meta);
        }

        /* ---------- GROUP BY 절 ---------- */
        // GROUP BY 항목이 있는 경우
        if (ps.getGroupBy() != null && ps.getGroupBy().getGroupByExpressions() != null) {
            for (Expression e : ps.getGroupBy().getGroupByExpressions()) {
            	// GROUP BY 항목 Expression 수집
                collectExpr(e, meta);
            }
        }

        /* ---------- HAVING 절 ---------- */
        // HAVING 조건이 있는 경우
        if (ps.getHaving() != null) {
        	// HAVING 조건 Expression 수집
            collectExpr(ps.getHaving(), meta);
        }
        
        /*
         * SELECT col1 FROM tableA ORDER BY 1
         * 
         * 아래 ORDER BY 로직은 위 예시 쿼리처럼 개별 SELECT 블록 또는 서브쿼리의 정렬을 의미
         */
        /* ---------- ORDER BY 절 ---------- */
        // // ORDER BY 항목이 있는 경우
        if (ps.getOrderByElements() != null) {
            for (OrderByElement obe : ps.getOrderByElements()) {
                if (obe.getExpression() != null) {
                	// ORDER BY 항목 Expression 수집
                    collectExpr(obe.getExpression(), meta);
                }
            }
        }
    }
    
    /*
     * - net.sf.jsqlparser.statement.select.FromItem
     * 		- SQL SELECT 쿼리에서 데이터의 출처(Source)를 나타내는 추상 클래스
     * 		- FROM 절이나 JOIN 절의 오른쪽에 등장하여 데이터를 가져오는 대상(테이블, 서브쿼리 등)을 정의
     * 		- 역할: 그 자체가 인스턴스로 사용되기보다는, 실제 데이터 출처를 정의하는 다양한 구체적인 클래스들의 부모 역할을 함
     * 		- 하위 클래스
     * 			- Table
     * 				- SQL 구문 형태: FROM customer c
     * 				- 가장 일반적인 형태. 실제 데이터베이스 테이블을 나타냄
     * 			- SubSelect
     * 				- SQL 구문 형태: FROM (SELECT ...) s
     * 				- 괄호로 묶인 서브쿼리(SELECT 구문)를 데이터 출처로 사용
     * 			- LateralSubSelect
     * 				- SQL 구문 형태: FROM ... LATERAL (SELECT ...)
     * 				- Oracle/PostgreSQL 등에서 지원하는 LATERAL 키워드와 함께 사용되며, 이전 FROM 절의 칼럼을 참조할 수 있는 서브쿼리임
     * 			- ValuesList
     * 				- SQL 구문 형태: FROM (VALUES (1, 'A'), (2, 'B')) t
     * 				- 인라인으로 정의된 값 목록(가상 테이블)을 데이터 출처로 사용
     * 			- ParenthesisFromItem
     * 				- SQL 구문 형태: FROM (tableA JOIN tableB)
     * 				- FROM 절 내에서 조인 그룹을 괄호로 묶어 우선순위를 지정할 때 사용
     * 			- FunctionItem
     * 				- SQL 구문 형태: FROM TABLE(function_name(...))
     * 				- 테이블을 반환하는 함수(Table-Valued Function)를 데이터 출처로 사용할 때 쓰임
     */
    /** FROM Item 처리 **/
    private void handleFromItem(FromItem item, SqlMeta parentMeta) {

    	// FROM Item이 일반 테이블(Table)인 경우
        if (item instanceof Table) {
            Table t = (Table) item;

            String tableName = t.getName();
            // 루트 테이블 목록에 추가 (최상위 FROM에 위치한 테이블)
            parentMeta.addRootTable(tableName);
            // 전체 테이블 목록에 추가
            parentMeta.addTable(tableName);

            // 테이블에 별칭(Alias)이 있는 경우 별칭 정보 추가
            if (t.getAlias() != null) {
                parentMeta.addAlias(t.getAlias().getName(), tableName);
            }

        } 
        // FROM Item이 서브쿼리(SubSelect)인 경우
        else if (item instanceof SubSelect) {
            SubSelect ss = (SubSelect) item;

            // 테이블 이름 대신 특수 마커 "__SUBQUERY__" 추가
            parentMeta.addTable("__SUBQUERY__");

            // 서브쿼리에 별칭이 있는 경우 별칭 정보 추가
            if (ss.getAlias() != null) {
                parentMeta.addAlias(ss.getAlias().getName(), "__SUBQUERY__");
            }

            // 서브쿼리 내부의 메타 정보를 수집하기 위한 새 리스트 생성
            List<SqlMeta> subMetas = new ArrayList<>();
            // 서브쿼리의 SelectBody를 재귀적으로 처리
            handleSelectBody(ss.getSelectBody(), subMetas);

            // 서브쿼리 내부의 위험/조건 마킹을 상위(parent) 메타 정보로 전파
            for (SqlMeta sub : subMetas) {
                if (sub.hasDangerousOrPredicate()) parentMeta.markDangerousOr();
                if (sub.hasUnsafeOrPredicate()) parentMeta.markUnsafeOr();
                if (sub.hasJoinOrWhereCondition()) parentMeta.markCondition();
            }
        }
    }

    /** Expression 수집 **/
    private void collectExpr(Expression expr, SqlMeta meta) {

    	// Expression이 NULL인 경우 무시
        if (expr == null) return;

        // Expression이 괄호(Parenthesis)로 묶여 있는 경우: 괄호 내부의 Expression을 재귀적으로 처리
        if (expr instanceof Parenthesis) {
            collectExpr(((Parenthesis) expr).getExpression(), meta);
            
            return;
        }
        
        /*
         * SQL에서 OR 연산자는 두 개의 불리언(Boolean) 표현식을 연결하여, 
         * 두 표현식 중 하나라도 참(True)이면 전체가 참이 되도록 만듦
         * 좌항이나 우항 중 컬럼 = 값 형태가 아닌 값 = 값 (예: '1'='1')과 같은 상수 비교가 포함되어 있는지 확인
         * 이러한 패턴은 SQL Injection 공격에 악용될 수 있으므로 meta.markUnsafeOr()로 마킹
         */
        // Expression이 OR 연산자인 경우 (보안 위험 패턴)
        if (expr instanceof OrExpression) {
        	// 위험한 OR 조건이 포함되었음을 마킹
            meta.markDangerousOr();

            // OR 연산자
            OrExpression or = (OrExpression) expr;
            // OR 연산자를 기준으로 OR 연산자의 좌항
            Expression left = or.getLeftExpression();
            // OR 연산자를 기준으로 OR 연산자의 우항
            Expression right = or.getRightExpression();

            // 좌항이나 우항 중 컬럼 = 값 형태가 아닌 값 = 값 (예: '1'='1')과 같은 상수 비교가 포함되어 있는지 확인
            if (isConstantComparison(left) || isConstantComparison(right)) {
                meta.markUnsafeOr();
            }

            // 좌항과 우항을 재귀적으로 수집
            collectExpr(left, meta);
            collectExpr(right, meta);
            
            return;
        }

        // Expression이 EXISTS 연산자인 경우
        if (expr instanceof ExistsExpression) {
        	// EXISTS는 조건(Condition)이므로 마킹
            meta.markCondition();
            // EXISTS 오른쪽의 서브쿼리/표현식을 재귀적으로 처리
            collectExpr(((ExistsExpression) expr).getRightExpression(), meta);
            
            return;
        }

        // Expression이 서브쿼리(SubSelect)인 경우 (IN, EXISTS 등 내부)
        if (expr instanceof SubSelect) {
            SubSelect ss = (SubSelect) expr;

            List<SqlMeta> subMetas = new ArrayList<>();
            // 서브쿼리 본문 처리
            handleSelectBody(ss.getSelectBody(), subMetas);

            // 서브쿼리 내부의 위험/조건 마킹을 현재 메타 정보로 전파
            for (SqlMeta sub : subMetas) {
                if (sub.hasDangerousOrPredicate()) meta.markDangerousOr();
                if (sub.hasUnsafeOrPredicate()) meta.markUnsafeOr();
                if (sub.hasJoinOrWhereCondition()) meta.markCondition();
            }
            
            return;
        }

        // OR 밖에서도 상수 비교는 unsafe로 마킹 (SQL Injection 공격: EX. `WHERE '1'='1'`)
        if (isConstantComparison(expr)) {
            meta.markUnsafeOr();
        }

        // Expression이 칼럼(Column)인 경우: 칼럼 목록에 추가
        if (expr instanceof Column) {
            meta.addColumn(expr.toString());
            
            return;
        }

        // Expression이 함수(Function)인 경우
        if (expr instanceof Function) {
            Function f = (Function) expr;
            // 함수 이름 목록에 추가
            meta.addExpression(f.getName());
            // 함수의 인자(Parameters)를 재귀적으로 처리
            if (f.getParameters() != null && f.getParameters().getExpressions() != null) {
                for (Expression p : f.getParameters().getExpressions()) {
                    collectExpr(p, meta);
                }
            }
            
            return;
        }
        /*
         * IN 연산자는 아래와 같은 형태이기 때문에 우항은 이미 다른 수집로직에서 처리되었거나 무시되었음.
         * 따라서 좌항만 수집함
         * WHERE user_id IN (101, 102, 103)
         * WHERE user_id IN (SELECT id FROM vip_users WHERE region = 'SEOUL')
         */
        // Expression이 IN 연산자인 경우: 좌항만 수집 (우항은 리터럴 리스트 또는 SubSelect로 이미 처리됨)
        if (expr instanceof InExpression) {
            InExpression in = (InExpression) expr;
            collectExpr(in.getLeftExpression(), meta);
            
            return;
        }

        // Expression이 이항 연산자(BinaryExpression)인 경우 (예: +, =, >, AND 등)
        if (expr instanceof BinaryExpression) {
            BinaryExpression be = (BinaryExpression) expr;
            // 좌항과 우항을 재귀적으로 처리
            collectExpr(be.getLeftExpression(), meta);
            collectExpr(be.getRightExpression(), meta);
            
            return;
        }

        // Expression이 CASE 문(CaseExpression)인 경우
        if (expr instanceof CaseExpression) {
            CaseExpression ce = (CaseExpression) expr;
            // SWITCH/WHEN/ELSE 항목들을 재귀적으로 처리
            if (ce.getSwitchExpression() != null)
                collectExpr(ce.getSwitchExpression(), meta);
            if (ce.getWhenClauses() != null)
                ce.getWhenClauses().forEach(e -> collectExpr(e, meta));
            if (ce.getElseExpression() != null)
                collectExpr(ce.getElseExpression(), meta);
        }
    }

    /** Unsafe 판단 헬퍼 **/
    // 두 피연산자가 모두 상수(Literal)인 비교 연산인지 확인하는 헬퍼 함수
    private boolean isConstantComparison(Expression expr) {

    	// 괄호 풀기
        Expression e = unwrap(expr);
        // 이항 연산자가 아니면 상수 비교가 아님
        if (!(e instanceof BinaryExpression)) return false;

        // OR/AND 같은 논리 연산자 자체는 상수 비교로 보지 않음 (최하위 비교만 확인)
        if (e instanceof OrExpression ||
            e instanceof net.sf.jsqlparser.expression.operators.conditional.AndExpression) {
            return false;
        }

        BinaryExpression be = (BinaryExpression) e;
        // 좌항과 우항의 괄호를 풀기
        Expression l = unwrap(be.getLeftExpression());
        Expression r = unwrap(be.getRightExpression());

        // 좌항과 우항이 모두 리터럴(상수 값)인 경우만 True 반환 (예: '1' = '1')
        return isLiteral(l) && isLiteral(r);
    }

    /** 괄호를 제거하는 헬퍼 함수 **/
    private Expression unwrap(Expression e) {
        return (e instanceof Parenthesis)
                ? ((Parenthesis) e).getExpression()
                : e;
    }
    /*
     * NULL도 상수 리터럴로 간주하는 이유
     * - SQL 논리적 관점: 쿼리가 실행될 때마다 값이 변하지 않는 고정된 상수로 작동. 
     *   따라서 JSqlParser를 포함한 대부분의 SQL 파서는 NULL을 리터럴(상수 값)을 나타내는 클래스(NullValue)로 처리
     *   
     * - 보안 검증 관점 (핵심 이유): SQL Injection 시도에 사용되는 상수 비교 패턴을 식별할 때
     *   'OR user_id = '123' OR 'A' = NULL' 과 형태에서 NULL을 상수로 마킹하지 않으면 상수 비교를 하지 못해 
     *   우회 성공할 수 있음. 
     *   NULL을 리터럴로 취급해야만 양쪽 피연산자가 모두 변수(칼럼) 참조가 아닌 상수/마커로 구성된 비교라는 것을 
     *   정확하게 판단하고 Unsafe 플래그를 마킹할 수 있음
     */
    /** 해당 Expression이 리터럴(상수) 값인지 확인하는 헬퍼 함수 **/
    private boolean isLiteral(Expression e) {
        return e instanceof StringValue
                || e instanceof LongValue
                || e instanceof DoubleValue
                || e instanceof DateValue
                || e instanceof TimeValue
                || e instanceof TimestampValue
                || e instanceof NullValue; // NULL도 상수 리터럴로 간주
    }
}
