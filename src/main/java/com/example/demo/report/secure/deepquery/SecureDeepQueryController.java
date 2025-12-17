package com.example.demo.report.secure.deepquery;

import com.example.demo.report.secure.dto.SecureDeepQueryRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/report/secure/deepquery")
public class SecureDeepQueryController {

    private final SecureDeepQueryService service;

    public SecureDeepQueryController(SecureDeepQueryService service) {
        this.service = service;
    }

    @PostMapping("/execute")
    public List<Map<String, Object>> execute(
            @RequestBody SecureDeepQueryRequest req
    ) {
        return service.execute(req);
    }
}
