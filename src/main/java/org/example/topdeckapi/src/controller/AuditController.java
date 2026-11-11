package org.example.topdeckapi.src.controller;

import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;


import org.example.topdeckapi.src.DTOs.response.PaginacionResponse;
import org.example.topdeckapi.src.model.Audit;
import org.example.topdeckapi.src.service.IMPL.AuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    /** filtro = action (kept for older clients); accion wins when both are sent. */
    @GetMapping("/admin/obtenerPaginados")
    public ResponseEntity<PaginacionResponse<Audit>> obtenerPaginados(
            @RequestParam(defaultValue = "1") Integer pagina,
            @RequestParam(defaultValue = "20") Integer tamanio,
            @RequestParam(defaultValue = "") String filtro,
            @RequestParam(required = false) String accion,
            @RequestParam(required = false) String tabla,
            @RequestParam(required = false) String usuario,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta
    ) {
        String accionFinal = (accion != null && !accion.isBlank()) ? accion : filtro;
        return ResponseEntity.ok(auditService.obtenerPaginados(pagina, tamanio, accionFinal, tabla, usuario, desde, hasta));
    }
}
