package fi.jannetahkola.palikka.core.api.exception.model;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;

public class ServerErrorModel extends ErrorModel {
    public ServerErrorModel(String message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }

    public ServerErrorModel(Exception e) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }

    @Schema(
            description = "HTTP status",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "INTERNAL_SERVER_ERROR")
    @Override
    public String getStatus() {
        return super.getStatusCode().toString();
    }

    @Schema(
            description = "Error message",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "Something went wrong")
    @Override
    public String getMessage() {
        return super.getMessage();
    }
}
