package org.example.topdeckapi.src.Security;

import jakarta.transaction.Transactional;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AuditorAwareIMPL implements AuditorAware<String> {
    @Override
    @Transactional
    public Optional<String> getCurrentAuditor() {
        Authentication authentication =  SecurityContextHolder.getContext().getAuthentication();

        if(authentication==null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken){
            return Optional.of("SISTEMA");
        }

        return Optional.of(authentication.getName());
    }
}
