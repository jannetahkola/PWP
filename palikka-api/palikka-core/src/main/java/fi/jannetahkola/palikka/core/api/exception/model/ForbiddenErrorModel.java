package fi.jannetahkola.palikka.core.api.exception.model;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;

public class ForbiddenErrorModel extends ErrorModel {
    public ForbiddenErrorModel(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }

    public ForbiddenErrorModel(Exception e) {
        super(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @Schema(
            description = "HTTP status",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "FORBIDDEN")
    @Override
    public String getStatus() {
        return super.getStatusCode().toString();
    }

    @Schema(
            description = "Error message",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "Access denied")
    @Override
    public String getMessage() {
        return super.getMessage();
    }
}
