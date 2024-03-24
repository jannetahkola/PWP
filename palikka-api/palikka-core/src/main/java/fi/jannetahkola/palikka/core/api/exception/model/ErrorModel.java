package fi.jannetahkola.palikka.core.api.exception.model;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Getter;
import org.springframework.http.HttpStatusCode;

@Data
@Getter
public abstract class ErrorModel {
    @Hidden
    private final HttpStatusCode statusCode;

    @Schema(
            description = "Error message",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "Invalid request")
    @NotNull
    private final String message;

    @Schema(
            description = "HTTP status",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "BAD_REQUEST")
    @NotNull
    private final String status;

    ErrorModel(HttpStatusCode statusCode, String message) {
        this.statusCode = statusCode;
        this.status = statusCode.toString();
        this.message = message;
    }
}
