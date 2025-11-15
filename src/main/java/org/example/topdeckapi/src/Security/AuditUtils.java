package org.example.topdeckapi.src.Security;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

@Component
@Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class AuditUtils {
    @PersistenceContext
    private EntityManager entityManager;

    private final AuditorAware<String> auditorAware;

    public AuditUtils(AuditorAware<String> auditorAware){
        this.auditorAware = auditorAware;
    }

    @Transactional
    public void setCurrentUserForAudit (){
        String username = auditorAware.getCurrentAuditor().orElse("SISTEMA");
        Query query = entityManager.createNativeQuery("SET @usuario_actual = :username");
        query.setParameter("username", username);
        query.executeUpdate();
    }
}
