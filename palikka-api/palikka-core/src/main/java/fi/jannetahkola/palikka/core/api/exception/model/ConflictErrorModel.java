package fi.jannetahkola.palikka.core.api.exception.model;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;

public class ConflictErrorModel extends ErrorModel {
    public ConflictErrorModel(String message) {
        super(HttpStatus.CONFLICT, message);
    }

    public ConflictErrorModel(Exception e) {
        super(HttpStatus.CONFLICT, e.getMessage());
    }

    @Schema(
            description = "HTTP status",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "CONFLICT")
    @Override
    public String getStatus() {
        return super.getStatusCode().toString();
    }

    @Schema(
            description = "Error message",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "Resource already exists")
    @Override
    public String getMessage() {
        return super.getMessage();
    }
}
