package com.example.demo.securesql.interceptor;

import com.example.demo.securesql.validator.OracleValidator;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;

import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.util.Properties;

/**
 * MyBatis SQL 실행 직전에 SQL 보안 검증을 수행하는 Interceptor.
 *
 * - Executor.query() 가 호출되기 직전에 SQL 문자열을 가로챔
 * - OracleValidator.validate(sql) 호출
 * - 검증 실패 시 RuntimeException 발생 → SQL 실행 차단
 */
@Intercepts({
    @Signature(
        type = Executor.class,     // 가로챌 대상 클래스 타입: SQL 실행의 핵심인 Executor
        method = "query",          // 가로챌 대상 메서드 이름: SELECT 구문 실행 시 호출되는 query 메서드
        args = {                   // 가로챌 메서드의 인자 타입 (메서드 오버로딩 구분을 위해 필수)
            MappedStatement.class, // 첫 번째 인자: 실행할 SQL 정보
            Object.class,          // 두 번째 인자: SQL에 바인딩할 파라미터 객체
            RowBounds.class,       // 세 번째 인자: 페이징 정보
            ResultHandler.class    // 네 번째 인자: 결과 처리 핸들러
        }
    )
})
public class SqlSecurityInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {

        // Invocation 객체로부터 가로챈 메서드의 인자들을 추출
        // invocation.getArgs()는 @Signature에 정의된 순서대로 인자를 배열로 반환
        // MappedStatement 추출
        MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
        // SQL 파라미터 객체 추출
        Object parameterObject = invocation.getArgs()[1];

        // 최종 실행 SQL 획득 (파라미터 바인딩 전 SQL)
        BoundSql boundSql = ms.getBoundSql(parameterObject);
        // BoundSql 객체로부터 파라미터가 바인딩될 SQL 문자열을 획득
        String sql = boundSql.getSql();

        // SQL 보안 검증
        // 획득한 SQL 문자열을 OracleValidator를 사용하여 검증
        // 이 메서드 내에서 파싱 및 화이트리스트 검사 등이 수행되며, 검증 실패 시 예외가 발생
        OracleValidator.validate(sql);

        // 검증 통과 시, 원래 가로챘던 Executor.query() 메서드를 실제 실행
        // 이 결과를 호출한 서비스 계층으로 반환
        return invocation.proceed();
    }

    /*
     * plugin 메서드는 MyBatis가 내부적으로 프록시(Proxy) 객체를 만들 때 사용
     * 대상 객체(target)가 현재 인터셉터에 의해 가로채져야 하는지 판단하고 래핑
     */
    @Override
    public Object plugin(Object target) {
    	// Plugin.wrap()은 target이 @Intercepts 어노테이션에 정의된 타입(Executor)에 해당하는지 확인하고,
        // 해당하면 이 Interceptor를 적용한 프록시 객체를 반환
        return Plugin.wrap(target, this);
    }

    /*
     * properties 는 필요 없지만 Interceptor 인터페이스 구현상 필수
     * 인터셉터 인스턴스 생성 시점에 외부 설정 파일 등으로부터 속성을 주입받을 때 사용
     */
    @Override
    public void setProperties(Properties properties) {
        // 현재 로직에서는 사용되지 않으므로 아무 작업도 하지 않음 (no-op: No Operation)
    }
}
