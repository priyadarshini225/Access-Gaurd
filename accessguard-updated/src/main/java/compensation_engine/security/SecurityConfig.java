package compensation_engine.security;

import compensation_engine.model.UserAccount;
import compensation_engine.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Optional;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            @Value("${security.enabled:false}") boolean securityEnabled) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        if (securityEnabled) {
            http.authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/compensation/health", "/h2-console/**", "/", "/index.html", "/css/**", "/js/**").permitAll()
                    .requestMatchers("/api/admin/**", "/api/users/**", "/api/policies/**").hasRole("ADMIN")
                    .requestMatchers("/api/audit/**").hasAnyRole("SECURITY_AUDITOR", "AUDITOR", "ADMIN")
                    .requestMatchers("/api/approvals/*/approve", "/api/approvals/*/reject")
                        .hasAnyRole("APPROVER", "ADMIN")
                    .requestMatchers("/api/approvals/*/execute", "/api/remediation/tasks/*/execute")
                        .hasAnyRole("OPERATOR", "ADMIN")
                    .requestMatchers("/api/remediation/tasks/*/approve")
                        .hasAnyRole("APPROVER", "ADMIN")
                    .requestMatchers("/api/remediation/**", "/api/reconciliation/**", "/api/access-expiry/**")
                        .hasAnyRole("OPERATOR", "SECURITY_AUDITOR", "AUDITOR", "ADMIN")
                    .requestMatchers("/api/security/**", "/api/identity/**").hasAnyRole("SECURITY", "OPERATOR", "ADMIN")
                    .anyRequest().authenticated())
                    .httpBasic(basic -> {});
        } else {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        }

        http.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder,
            @Value("${security.users.admin.password:admin123}") String adminPassword,
            @Value("${security.users.operator.password:operator123}") String operatorPassword,
            @Value("${security.users.auditor.password:auditor123}") String auditorPassword) {
        return username -> {
            // Check property-defined test overrides first
            if ("admin".equalsIgnoreCase(username) && !"admin123".equals(adminPassword)) {
                return User.withUsername("admin").password(passwordEncoder.encode(adminPassword)).roles("ADMIN", "SECURITY", "APPROVER", "OPERATOR", "AUDITOR", "SECURITY_AUDITOR").build();
            } else if ("operator".equalsIgnoreCase(username) && !"operator123".equals(operatorPassword)) {
                return User.withUsername("operator").password(passwordEncoder.encode(operatorPassword)).roles("OPERATOR", "APPROVER").build();
            } else if ("auditor".equalsIgnoreCase(username) && !"auditor123".equals(auditorPassword)) {
                return User.withUsername("auditor").password(passwordEncoder.encode(auditorPassword)).roles("AUDITOR", "SECURITY_AUDITOR").build();
            }

            // Check database user account
            Optional<UserAccount> accountOpt = userAccountRepository.findByUsername(username);
            if (accountOpt.isPresent()) {
                UserAccount account = accountOpt.get();
                return User.withUsername(account.getUsername())
                        .password(account.getPasswordHash())
                        .roles(account.getRole())
                        .disabled(!account.isEnabled())
                        .build();
            }

            // Fallback for default test users if DB is empty
            if ("admin".equalsIgnoreCase(username)) {
                return User.withUsername("admin").password(passwordEncoder.encode(adminPassword)).roles("ADMIN", "SECURITY", "APPROVER", "OPERATOR", "AUDITOR", "SECURITY_AUDITOR").build();
            } else if ("operator".equalsIgnoreCase(username)) {
                return User.withUsername("operator").password(passwordEncoder.encode(operatorPassword)).roles("OPERATOR", "APPROVER").build();
            } else if ("auditor".equalsIgnoreCase(username)) {
                return User.withUsername("auditor").password(passwordEncoder.encode(auditorPassword)).roles("AUDITOR", "SECURITY_AUDITOR").build();
            }

            throw new UsernameNotFoundException("User not found: " + username);
        };
    }
}