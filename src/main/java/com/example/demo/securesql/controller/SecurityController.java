package com.example.demo.securesql.controller;

import com.example.demo.securesql.service.SecureSqlService;
import com.example.demo.securesql.validator.SqlFuzzTester.FuzzResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sql")
public class SecurityController {

    private final SecureSqlService secureSqlService;

    public SecurityController(SecureSqlService secureSqlService) {
        this.secureSqlService = secureSqlService;
    }

    /** 임의 SQL 한 건 검증 (DB 미연결 mock) */
    @PostMapping("/validate")
    public Map<String, Object> validate(@RequestBody Map<String, String> body) {
        String sql = body.get("sql");
        Map<String, Object> res = new HashMap<>();
        try {
            secureSqlService.validate(sql);
            res.put("ok", true);
            res.put("message", "VALID");
        } catch (Exception e) {
            res.put("ok", false);
            res.put("message", e.getMessage());
        }
        res.put("sql", sql);
        return res;
    }

    /** SQL Injection fuzz 테스트 실행 */
    @GetMapping("/fuzz")
    public List<FuzzResult> fuzz() {
        return secureSqlService.runFuzz();
    }
}
