package com.cramer.config;

import com.cramer.util.JwtUtil;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * Test Security Configuration for @WebMvcTest controller tests.
 * Disables CSRF and allows all requests for testing purposes.
 * Authentication is handled via @WithMockUser or manual SecurityContext setup.
 * 
 * Provides mock beans for JwtUtil and JwtAuthFilter to avoid dependency issues.
 */
@TestConfiguration
@EnableWebMvc
public class TestSecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Disable CSRF for easier testing
            .authorizeHttpRequests(authorize -> authorize
                .anyRequest().authenticated() // All requests must be authenticated
            );

        return http.build();
    }

    /**
     * Provide a mock JwtUtil bean for tests.
     * This prevents the real JwtUtil from being instantiated (which needs supabase.jwt.secret).
     */
    @Bean
    @Primary
    public JwtUtil jwtUtil() {
        return Mockito.mock(JwtUtil.class);
    }

    /**
     * Provide a mock JwtAuthFilter bean for tests.
     * This prevents the filter from being added to the security chain during tests.
     */
    @Bean
    @Primary
    public JwtAuthFilter jwtAuthFilter() {
        return Mockito.mock(JwtAuthFilter.class);
    }
}
