package fi.jannetahkola.palikka.users.config;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.method.HandlerMethod;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

@Component
public class OpenApiConfig {
    @Bean
    OpenAPI openApiSpec() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("Users API")
                                .contact(
                                        new Contact()
                                                .name("Janne Tahkola")
                                                .email("janne.tahkola@gmail.com")
                                                .url("https://github.com/jannetahkola"))
                                .description("User & access management"))
                .components(
                        new Components()
                                .addSecuritySchemes(
                                        HttpHeaders.AUTHORIZATION,
                                        new SecurityScheme()
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("Bearer")
                                                .bearerFormat("JWT")
                                                .in(SecurityScheme.In.HEADER)
                                                .description("JWT access token")))
                .security(List.of(
                        new SecurityRequirement()
                                .addList(HttpHeaders.AUTHORIZATION)));
    }

    @Bean
    OpenApiCustomizer openApiCustomizer() {
        return spec -> spec.getComponents().getSchemas().putAll(ModelConverters.getInstance().read(ProblemDetail.class));
    }

    @Bean
    OperationCustomizer operationCustomizer() {
        // Checks if there's an injected authorization header in the method. If the parameter is added to such method,
        // there would be two of them in the OpenAPI spec.
        final Predicate<HandlerMethod> usesAuthorizationHeader = handlerMethod -> Arrays
                .stream(handlerMethod.getMethodParameters())
                .anyMatch(methodParameter -> {
                    RequestHeader requestHeader = methodParameter.getParameterAnnotation(RequestHeader.class);
                    return requestHeader != null && requestHeader.value().equals(HttpHeaders.AUTHORIZATION);
                });

        Schema<ProblemDetail> problemDetailSchema  = new Schema<>();
        problemDetailSchema.setName("ProblemDetail");
        problemDetailSchema.set$ref("#/components/schemas/ProblemDetail");

        return (operation, handlerMethod) -> {
            if (!handlerMethod.hasMethodAnnotation(SecurityRequirements.class)) {
                // Add 403 response if not marked as open with SecurityRequirements
                operation
                        .getResponses()
                        .addApiResponse("403", apiResponseOf("Forbidden", problemDetailSchema));
                if (!usesAuthorizationHeader.test(handlerMethod)) {
                    operation
                            .addParametersItem(
                                    new Parameter()
                                            .in("header")
                                            .required(true)
                                            .name(HttpHeaders.AUTHORIZATION)
                                            .description("Bearer access token")
                                            .schema(new Schema<>().type("string"))
                                            .example("Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzUxMiIsInB0..."));
                }
            }
            if (!handlerMethod.hasMethodAnnotation(GetMapping.class)
                    && !handlerMethod.hasMethodAnnotation(DeleteMapping.class)) {
                // Supported Content-Type required when not GET or DELETE
                operation
                        .addParametersItem(
                                new Parameter()
                                        .in("header")
                                        .required(true)
                                        .name(HttpHeaders.CONTENT_TYPE)
                                        .description("JSON content type")
                                        .schema(new Schema<>().type("string"))
                                        .example(org.springframework.http.MediaType.APPLICATION_JSON_VALUE))
                        .getResponses()
                        .addApiResponse("415", apiResponseOf("Unsupported Media Type", problemDetailSchema));
            }
            // These can happen anywhere
            operation.getResponses()
                    .addApiResponse("405", apiResponseOf("Method Not Allowed", problemDetailSchema))
                    .addApiResponse("406", apiResponseOf("Not Acceptable", problemDetailSchema))
                    .addApiResponse("500", apiResponseOf("Internal Server Error", problemDetailSchema));
            return operation;
        };
    }

    private <T> ApiResponse apiResponseOf(String description, Schema<T> schema) {
        return new ApiResponse()
                .description(description)
                .content(
                        new Content()
                                .addMediaType(
                                        org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                        new MediaType().schema(schema)));
    }
}
