package com.example.demo.securesql.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * SQL 보안 검증이 반드시 필요한 Mapper 메서드에 명시적으로 선언하는 애노테이션.
 *
 * - ${} 사용
 * - 동적 컬럼 / ORDER BY / GROUP BY
 * - 사용자 입력이 SQL 구조에 영향을 미치는 경우
 *
 * 해당 애노테이션이 붙은 SQL만 SqlSecurityInterceptor에서 검증 대상이 됨.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SecureSqlRequired {
}
