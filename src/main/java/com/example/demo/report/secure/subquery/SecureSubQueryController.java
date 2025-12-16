package com.example.demo.report.secure.subquery;

import com.example.demo.report.secure.dto.SecureEnterpriseRiskRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/report/secure/subquery")
public class SecureSubQueryController {

    private final SecureSubQueryService service;

    public SecureSubQueryController(SecureSubQueryService service) {
        this.service = service;
    }

    @PostMapping("/enterprise-risk")
    public List<Map<String, Object>> report(
            @RequestBody SecureEnterpriseRiskRequest req
    ) {
        return service.execute(req);
    }
}
