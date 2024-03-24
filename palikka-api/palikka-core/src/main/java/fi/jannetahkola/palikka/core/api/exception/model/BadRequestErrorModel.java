package fi.jannetahkola.palikka.core.api.exception.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@EqualsAndHashCode(callSuper = true)
public class BadRequestErrorModel extends ErrorModel {
    public BadRequestErrorModel(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }

    public BadRequestErrorModel(Exception e) {
        super(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @Schema(
            description = "HTTP status",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "BAD_REQUEST")
    @Override
    public String getStatus() {
        return super.getStatusCode().toString();
    }
}
