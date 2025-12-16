package com.example.demo.report.secure.cte;

import com.example.demo.report.secure.dto.SecureEnterpriseRiskRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/report/secure/cte")
public class SecureCteController {

    private final SecureCteService service;

    public SecureCteController(SecureCteService service) {
        this.service = service;
    }

    @PostMapping("/enterprise-risk")
    public List<Map<String, Object>> report(
            @RequestBody SecureEnterpriseRiskRequest req
    ) {
        return service.execute(req);
    }
}
