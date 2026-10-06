package ru.library.config;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.*;
import org.springframework.context.annotation.*;
import org.springdoc.core.customizers.OpenApiCustomizer;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.responses.ApiResponse;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI libraryOpenApi() {
        return new OpenAPI().info(new Info().title("Библиотека").version("1.0")
            .description("Каталог книг. USER читает данные, ADMIN управляет каталогом. Получите JWT через /api/auth/login."))
            .components(new Components().addSecuritySchemes("bearerAuth",
                new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }

    @Bean
    OpenApiCustomizer errorResponses() {
        return api -> {
            var schemas = io.swagger.v3.core.converter.ModelConverters.getInstance()
                .read(ru.library.exception.ErrorResponse.class);
            schemas.forEach((name, schema) -> api.getComponents().addSchemas(name, schema));
            api.getPaths().values().forEach(path -> path.readOperations().forEach(operation -> {
                for (String status : new String[]{"400", "401", "403", "404", "405", "406", "409", "415", "500"}) {
                    operation.getResponses().addApiResponse(status, new ApiResponse()
                        .description(switch (status) {
                            case "400" -> "Некорректные данные";
                            case "401" -> "Требуется авторизация";
                            case "403" -> "Недостаточно прав";
                            case "404" -> "Запись не найдена";
                            case "405" -> "Метод не поддерживается";
                            case "406" -> "Неподдерживаемый формат ответа";
                            case "409" -> "Конфликт данных";
                            case "415" -> "Неподдерживаемый Content-Type";
                            default -> "Ошибка сервера";
                        }).content(new Content().addMediaType("application/json", new MediaType()
                            .schema(new Schema<>().$ref("#/components/schemas/ErrorResponse")))));
                }
            }));
        };
    }
}
