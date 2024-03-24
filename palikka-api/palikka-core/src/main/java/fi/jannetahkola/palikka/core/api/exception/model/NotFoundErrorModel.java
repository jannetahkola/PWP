package fi.jannetahkola.palikka.core.api.exception.model;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;

public class NotFoundErrorModel extends ErrorModel {
    public NotFoundErrorModel(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }

    public NotFoundErrorModel(Exception e) {
        super(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @Schema(
            description = "HTTP status",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "NOT_FOUND")
    @Override
    public String getStatus() {
        return super.getStatusCode().toString();
    }

    @Schema(
            description = "Error message",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "Resource not found")
    @Override
    public String getMessage() {
        return super.getMessage();
    }
}
