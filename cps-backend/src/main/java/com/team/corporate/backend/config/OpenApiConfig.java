package com.team.corporate.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {
    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI procurementApi() {
        return new OpenAPI()
                .info(new Info().title("Система корпоративных закупок").version("1.0")
                        .description("Войдите через POST /api/auth/login и вставьте accessToken в Authorize. "
                                + "Временный пароль необходимо сменить через PUT /api/auth/password; "
                                + "после смены требуется повторный вход. Административные операции доступны ADMIN. "
                                + "Ошибки возвращаются в формате Problem Detail с Correlation ID."))
                .components(new Components().addSecuritySchemes(BEARER_AUTH,
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }
}
