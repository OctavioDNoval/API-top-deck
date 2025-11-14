package org.example.topdeckapi.src.controller;


import org.example.topdeckapi.src.model.Audit;
import org.example.topdeckapi.src.service.IMPL.AuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/audit")
public class AuditController {
    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/admin/getAll")
    public ResponseEntity<List<Audit>> getAll(){
        List<Audit> audits = auditService.getAll();
        return ResponseEntity.ok(audits);
    }
}
