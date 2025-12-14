package com.example.demo.securesql.config;

import com.example.demo.securesql.interceptor.SqlSecurityInterceptor;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

/**
 * MyBatis + SQL 보안 Interceptor 설정 클래스
 *
 * - SqlSecurityInterceptor 를 MyBatis Plugin 으로 등록
 * - 모든 Mapper XML / Mapper Interface SQL 에 자동 적용
 */
@Configuration
public class MyBatisSecurityConfig {

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {

		// Spring에서 MyBatis의 SqlSessionFactory를 생성하는 팩토리 빈 객체 생성
        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        // 팩토리에 데이터 소스를 설정. MyBatis가 이 DataSource를 사용하여 DB에 연결
        factory.setDataSource(dataSource);
        
        // SqlSessionFactory를 직접 만들 경우 application.properties의 mybatis.mapper-locations 무시됨
        // 따라서 xml 파일의 path를 명시해줘야 함
        // @MapperScan → 인터페이스 등록
        // SqlSessionFactory → XML + SQL + Plugin 등록
        factory.setMapperLocations(
                new PathMatchingResourcePatternResolver()
                    .getResources("classpath:/mapper/**/*.xml")
            );

        // SQL 보안 Interceptor 등록
        // setPlugins() 메서드를 사용하여 MyBatis 플러그인(인터셉터) 목록을 설정
        factory.setPlugins(new Interceptor[] {
        	// 커스텀하게 정의한 SqlSecurityInterceptor 인스턴스를 배열에 담아 등록
        	// 이 인터셉터는 MyBatis의 SQL 실행 전/후 과정에 개입하여 보안 검증을 수행
            new SqlSecurityInterceptor()
        });

		// 팩토리 객체로부터 최종적으로 SqlSessionFactory 객체를 생성하여 반환
        return factory.getObject();
    }
}
