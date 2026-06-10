package com.cramer.config;

import static org.springframework.security.config.Customizer.withDefaults;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
        throws Exception {
        http
            // 1. Enable CORS and disable CSRF
            .cors(withDefaults())
            .csrf(csrf -> csrf.disable())
            // 2. Set session management to stateless
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            // 3. Set up authorization rules
            .authorizeHttpRequests(authorize ->
                authorize
                    // Allow async/error dispatches for SSE to avoid re-auth on committed responses
                    .dispatcherTypeMatchers(
                        DispatcherType.ASYNC,
                        DispatcherType.ERROR
                    )
                    .permitAll()
                    // Allow public access to auth, API docs, and WebSocket handshake endpoints
                    .requestMatchers(
                        "/api/auth/**",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/ws/**"
                    )
                    .permitAll()
                    // Allow framework error endpoint (avoids AccessDenied after SSE response commit)
                    .requestMatchers("/error")
                    .permitAll()
                    // Allow public access to PayOS webhook (must be accessible without auth)
                    .requestMatchers("/api/payments/webhook")
                    .permitAll()
                    // Allow public access to Lúa pack options
                    .requestMatchers("/api/payments/lua-packs")
                    .permitAll()
                    // Allow public access to PayOS config status check
                    .requestMatchers("/api/payments/config-status")
                    .permitAll()
                    // Admin APIs require server-side admin authority, not caller-supplied headers
                    .requestMatchers("/api/admin/**")
                    .hasRole("ADMIN")
                    // All other API requests must be authenticated
                    .requestMatchers("/api/**")
                    .authenticated()
                    // Fail closed: deny any other request by default
                    .anyRequest()
                    .authenticated()
            )
            // 4. Add our custom JWT filter
            .addFilterBefore(
                jwtAuthFilter,
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }
}
