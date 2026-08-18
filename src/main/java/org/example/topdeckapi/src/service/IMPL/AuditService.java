package org.example.topdeckapi.src.service.IMPL;

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

    private static final int DIAS_RETENCION_LOGS = 90;

    public List<Audit> getAll (){
        return repository.findAll();
    }

    public PaginacionResponse<Audit> obtenerPaginados(Integer pagina, Integer tamanio, String filtro) {
        if (pagina == null || pagina < 1) pagina = 1;
        if (tamanio == null || tamanio < 1) tamanio = 20;
        Pageable pageable = PageRequest.of(pagina - 1, tamanio, Sort.by(Sort.Direction.DESC, "idAuditoria"));

        Page<Audit> page = (filtro == null || filtro.trim().isEmpty())
                ? repository.findAll(pageable)
                : repository.findByAccionIgnoreCase(filtro.trim(), pageable);

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
            int borrados = eliminarAntiguos(DIAS_RETENCION_LOGS);
            if (borrados > 0) {
                log.info("Limpieza de auditoría: {} logs antiguos eliminados", borrados);
            }
        } catch (Exception e) {
            log.error("Error en la limpieza programada de auditoría", e);
        }
    }

    @Transactional
    public void registrar(String accion, String tabla) {
        Audit log = new Audit();
        log.setNombreUsuario(obtenerUsuarioActual());
        log.setFechaAudit(LocalDateTime.now());
        log.setAccion(accion);
        log.setTabla(tabla);
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
