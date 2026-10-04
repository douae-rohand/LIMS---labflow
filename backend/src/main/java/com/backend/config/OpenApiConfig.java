package com.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration Springdoc / OpenAPI 3.
 * Accessible via /swagger-ui/index.html et /v3/api-docs.
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";
    private static final String TENANT_HEADER_SCHEME = "tenantHeader";

    @Bean
    public OpenAPI limsOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("LIMS API")
                        .description("Laboratory Information Management System – API REST")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Équipe LIMS")
                                .email("support@lims.local"))
                        .license(new License()
                                .name("Proprietary")))
                .addSecurityItem(new SecurityRequirement()
                        .addList(SECURITY_SCHEME_NAME)
                        .addList(TENANT_HEADER_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT"))
                        .addSecuritySchemes(TENANT_HEADER_SCHEME,
                                new SecurityScheme()
                                        .name("X-Tenant-ID")
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .description("En-tête optionnel/requis du tenant (code ou nomSchema du laboratoire)")));
    }
}
