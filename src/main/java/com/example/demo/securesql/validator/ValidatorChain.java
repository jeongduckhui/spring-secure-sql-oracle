package com.example.demo.securesql.validator;

import com.example.demo.securesql.parser.SqlMeta;

import java.util.ArrayList;
import java.util.List;

public class ValidatorChain {

	// SqlValidator 인터페이스를 구현한 검증기들을 저장하는 리스트
    private final List<SqlValidator> validators = new ArrayList<>();

    /**
     * 검증 체인에 새로운 Validator를 추가.
     * Builder 패턴을 위해 ValidatorChain 객체 자신을 반환.
     *
     * @param v SqlValidator 인터페이스를 구현한 Validator 객체
     * @return 체인 자신
     */
    public ValidatorChain add(SqlValidator v) {
    	// 리스트에 Validator 추가
        validators.add(v);
        // 체인을 연속적으로 구성할 수 있도록 자기 자신(this)을 반환
        return this;
    }

    /**
     * 체인에 등록된 모든 Validator를 순차적으로 실행.
     * 하나의 Validator라도 RuntimeException을 발생시키면 검증은 즉시 중단됨.
     *
     * @param meta OracleAstParser를 통해 파싱된 SQL 쿼리의 메타 정보
     */
    public void validate(SqlMeta meta) {
    	// 등록된 Validator 목록을 순회
        for (SqlValidator v : validators) {
        	// 현재 Validator의 validate 메서드를 실행
        	// 실패 시 (RuntimeException 발생 시) 루프가 중단되고 예외가 호출자에게 전파됨
            v.validate(meta);
        }
    }
}
