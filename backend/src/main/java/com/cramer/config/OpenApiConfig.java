package com.cramer.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI cramerOpenAPI() {
        // Security scheme for JWT
        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Enter JWT token from Supabase Auth");

        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList("bearerAuth");

        return new OpenAPI()
                .info(new Info()
                        .title("Cramer API - IELTS Practice Platform")
                        .description("""
                                RESTful API for Cramer IELTS practice platform.
                                
                                **Features:**
                                - User profile management
                                - IELTS exam sections (Cambridge 17, 18, etc.)
                                - Questions with JSONB content support
                                - User answers tracking and analytics
                                - Performance statistics
                                
                                **Authentication:**
                                Use Supabase Auth JWT token in Authorization header:
                                ```
                                Authorization: Bearer <your-jwt-token>
                                ```
                                
                                **Database:**
                                PostgreSQL hosted on Supabase
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Cramer Team")
                                .email("support@cramer.com")
                                .url("https://github.com/leminhkhoa117/Cramer"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local Development Server"),
                        new Server()
                                .url("https://api.cramer.com")
                                .description("Production Server (Coming Soon)")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", securityScheme))
                .addSecurityItem(securityRequirement);
    }
}
