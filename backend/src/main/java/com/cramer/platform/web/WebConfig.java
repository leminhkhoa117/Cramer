package com.cramer.platform.web;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

/**
 * Web layer configuration (SPEC-04 §3, SPEC-18 §2): CORS policy and JSON serialization.
 *
 * <p><strong>Jackson:</strong> the classpath carries both Jackson 2 ({@code com.fasterxml},
 * required by Hypersistence JSONB + our {@code JsonNode} DTOs) and Jackson 3
 * ({@code tools.jackson}, pulled by {@code spring-boot-starter-jackson} / springdoc). Controller
 * (de)serialization is pinned to <strong>Jackson 2</strong> by stripping the auto-registered
 * Jackson converters and installing {@link MappingJackson2HttpMessageConverter} first, so
 * {@code com.fasterxml} {@code JsonNode} fields serialize as real JSON trees.
 *
 * <p>The user id is always derived from the verified JWT (SPEC-04 §1.2) — there is no
 * {@code X-User-Id} / {@code X-Debug-Key} header trust, so those are intentionally absent from
 * the allowed-header list.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final List<String> allowedOrigins;

    public WebConfig(@Value("${cramer.cors.allowed-origins:http://localhost:5173,http://localhost:3000,http://localhost:3001,http://127.0.0.1:5173,http://127.0.0.1:3000,https://cramer.vn,https://www.cramer.vn}") String origins) {
        this.allowedOrigins = Arrays.stream(origins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    /** Jackson 2 mapper for web (de)serialization and ad-hoc injection: ISO dates, lenient unknowns. */
    @Bean
    public ObjectMapper objectMapper() {
        return JsonMapper.builder()
                .findAndAddModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }

    /**
     * Force controller JSON onto Jackson 2: drop every auto-registered Jackson converter (incl.
     * the Jackson 3 {@code JacksonJsonHttpMessageConverter}) and put the Jackson 2 converter first.
     */
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.removeIf(converter -> converter.getClass().getName().contains("Jackson"));
        converters.add(0, new MappingJackson2HttpMessageConverter(objectMapper()));
    }

    /** Single CORS source consumed by both Spring MVC and Spring Security ({@code http.cors(...)}). */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Cache-Control"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
