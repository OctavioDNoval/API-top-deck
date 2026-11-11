package org.example.topdeckapi.src.service.IMPL;

import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDate;
import java.util.ArrayList;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.topdeckapi.src.DTOs.response.PaginacionResponse;
import org.example.topdeckapi.src.Repository.IAuditRepository;
import org.example.topdeckapi.src.model.Audit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {
    private final IAuditRepository repository;
    private final PaginacionService paginacionService;

    // How long logs are kept (purged every night at 03:30).
    @Value("${app.audit.dias-retencion:90}")
    private int diasRetencion = 90;

    public List<Audit> getAll (){
        return repository.findAll();
    }

    /*
     * Newest first. Every filter is optional: accion and tabla match exactly,
     * usuario is a partial match, desde/hasta are inclusive days.
     */
    public PaginacionResponse<Audit> obtenerPaginados(Integer pagina, Integer tamanio, String accion, String tabla,
                                                      String usuario, LocalDate desde, LocalDate hasta) {
        if (pagina == null || pagina < 1) pagina = 1;
        if (tamanio == null || tamanio < 1) tamanio = 20;
        tamanio = Math.min(tamanio, 100);
        Pageable pageable = PageRequest.of(pagina - 1, tamanio, Sort.by(Sort.Direction.DESC, "idAuditoria"));

        Specification<Audit> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (accion != null && !accion.isBlank()) p.add(cb.equal(cb.upper(root.get("accion")), accion.trim().toUpperCase()));
            if (tabla != null && !tabla.isBlank()) p.add(cb.equal(cb.lower(root.get("tabla")), tabla.trim().toLowerCase()));
            if (usuario != null && !usuario.isBlank())
                p.add(cb.like(cb.lower(root.get("nombreUsuario")), "%" + usuario.trim().toLowerCase() + "%"));
            if (desde != null) p.add(cb.greaterThanOrEqualTo(root.get("fechaAudit"), desde.atStartOfDay()));
            if (hasta != null) p.add(cb.lessThan(root.get("fechaAudit"), hasta.plusDays(1).atStartOfDay()));
            return cb.and(p.toArray(new Predicate[0]));
        };
        Page<Audit> page = repository.findAll(spec, pageable);
        return paginacionService.crearPaginacionResponse(page, pagina, tamanio, Function.identity());
    }

    @Transactional
    public int eliminarAntiguos(int dias) {
        LocalDateTime corte = LocalDateTime.now().minusDays(dias);
        return repository.eliminarAntiguos(corte);
    }

    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void limpiezaProgramada() {
        try {
            int borrados = eliminarAntiguos(diasRetencion);
            if (borrados > 0) {
                log.info("Limpieza de auditoría: {} logs antiguos eliminados", borrados);
            }
        } catch (Exception e) {
            log.error("Error en la limpieza programada de auditoría", e);
        }
    }

    @Transactional
    public void registrar(String accion, String tabla) {
        registrar(accion, tabla, null, null);
    }

    /** idRegistro: uuid of the affected record. detalle: short summary (no personal data). */
    @Transactional
    public void registrar(String accion, String tabla, String idRegistro, String detalle) {
        Audit log = new Audit();
        log.setNombreUsuario(obtenerUsuarioActual());
        log.setFechaAudit(LocalDateTime.now());
        log.setAccion(accion);
        log.setTabla(tabla);
        log.setIdRegistro(idRegistro);
        log.setDetalle(detalle == null ? null : (detalle.length() > 500 ? detalle.substring(0, 497) + "..." : detalle));
        repository.save(log);
    }

    private String obtenerUsuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return "SISTEMA";
        }
        return auth.getName();
    }
}
