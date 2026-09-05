package com.project.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3 / Swagger Documentation Configuration.
 * Configures JWT Bearer authentication scheme and API metadata.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "SIH26188 - Fake Identity & Document Screening API",
                version = "1.0.0",
                description = "Secure central API gateway and security layer for SIH26188. " +
                        "Coordinates user authentication, RBAC, IDOR protection, document inspection, " +
                        "FastAPI AI integration, transparent risk scoring, and audit logging.",
                contact = @Contact(name = "Backend & Security Lead", email = "security@idshield.project"),
                license = @License(name = "Proprietary", url = "https://idshield.internal")
        )
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Provide the JWT Bearer access token received from /api/auth/login"
)
public class OpenApiConfig {
}

