package com.example.demo.securesql.service;

import com.example.demo.securesql.validator.OracleValidator;
import com.example.demo.securesql.validator.SqlFuzzTester;
import com.example.demo.securesql.validator.SqlFuzzTester.FuzzResult;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SecureSqlService {

    public void validate(String sql) {
        OracleValidator.validate(sql);
    }

    public List<FuzzResult> runFuzz() {
        return SqlFuzzTester.runDefaultFuzz();
    }
}
