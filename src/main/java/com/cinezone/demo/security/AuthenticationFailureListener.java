package com.cinezone.demo.security;

import com.cinezone.demo.model.entity.SecurityAudit;
import com.cinezone.demo.repository.SecurityAuditRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthenticationFailureListener implements ApplicationListener<AuthenticationFailureBadCredentialsEvent> {

    private final SecurityAuditRepository securityAuditRepository;
    private final HttpServletRequest request;

    @Override
    public void onApplicationEvent(AuthenticationFailureBadCredentialsEvent event) {
        String username = (String) event.getAuthentication().getPrincipal();
        String ip = request.getRemoteAddr();

        // Si la aplicación usa un proxy/load balancer, intentar obtener la IP real:
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            ip = forwardedFor.split(",")[0];
        }

        SecurityAudit audit = SecurityAudit.builder()
                .ip(ip)
                .correoIntentado(username)
                .evento("LOGIN_FAILED_BAD_CREDENTIALS")
                .build();

        securityAuditRepository.save(audit);
    }
}
