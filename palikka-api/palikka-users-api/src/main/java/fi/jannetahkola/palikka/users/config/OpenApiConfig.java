package fi.jannetahkola.palikka.users.config;

import fi.jannetahkola.palikka.core.api.exception.model.ForbiddenErrorModel;
import fi.jannetahkola.palikka.core.api.exception.model.ServerErrorModel;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
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
        return spec -> {
            // Add common responses
            spec.getComponents().getSchemas().putAll(ModelConverters.getInstance().read(ForbiddenErrorModel.class));
            spec.getComponents().getSchemas().putAll(ModelConverters.getInstance().read(ServerErrorModel.class));

            Schema<ForbiddenErrorModel> forbiddenErrorSchema = new Schema<>();
            forbiddenErrorSchema.setName("ForbiddenErrorModel");
            forbiddenErrorSchema.set$ref("#/components/schemas/ForbiddenErrorModel");

            Schema<ServerErrorModel> serverErrorSchema = new Schema<>();
            serverErrorSchema.setName("ServerErrorModel");
            serverErrorSchema.set$ref("#/components/schemas/ServerErrorModel");

            spec.getPaths().values()
                    .forEach(path -> path.readOperations()
                            .forEach(readOperation -> {
                                ApiResponses responses = readOperation.getResponses();
                                // Log in endpoint will never respond with 403 but not sure how to exclude it here
                                responses.addApiResponse("403", newApiResponse("Forbidden", forbiddenErrorSchema));
                                responses.addApiResponse("500", newApiResponse("Server error", serverErrorSchema));
                            }));
        };
    }

    @Bean
    OperationCustomizer operationCustomizer() {
        // Checks if there's an injected authorization header in the method. If the parameter is added to such method,
        // there would two of them in the OpenAPI spec.
        final Predicate<HandlerMethod> usesAuthorizationHeader = handlerMethod -> Arrays
                .stream(handlerMethod.getMethodParameters())
                .anyMatch(methodParameter -> {
                    RequestHeader requestHeader = methodParameter.getParameterAnnotation(RequestHeader.class);
                    return requestHeader != null && requestHeader.value().equals(HttpHeaders.AUTHORIZATION);
                });
        return (operation, handlerMethod) -> {
            if (!(handlerMethod.hasMethodAnnotation(SecurityRequirements.class)
                    || usesAuthorizationHeader.test(handlerMethod))) {
                // If not marked as open with SecurityRequirements and authorization header not injected, add
                // authorization parameter
                operation.addParametersItem(
                        new Parameter()
                                .in("header")
                                .required(true)
                                .name(HttpHeaders.AUTHORIZATION)
                                .description("Bearer access token")
                                .schema(new Schema<>().type("string"))
                                .example("Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzUxMiIsInB0...")
                );
            }
            // Content-Type always required
            return operation
                    .addParametersItem(
                            new Parameter()
                                    .in("header")
                                    .required(true)
                                    .name(HttpHeaders.CONTENT_TYPE)
                                    .description("JSON content type")
                                    .schema(new Schema<>().type("string"))
                                    .example(org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
                    );
        };
    }

    private <T> ApiResponse newApiResponse(String message, Schema<T> schema) {
        MediaType mediaType = new MediaType();
        mediaType.setSchema(schema);
        return new ApiResponse()
                .description(message)
                .content(
                        new Content()
                                .addMediaType(
                                        org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
                                        mediaType));
    }
}
