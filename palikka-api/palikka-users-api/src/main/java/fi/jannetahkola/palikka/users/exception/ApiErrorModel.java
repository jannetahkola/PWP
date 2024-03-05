package fi.jannetahkola.palikka.users.exception;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

@Data
@With
@AllArgsConstructor
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ApiErrorModel {
    private HttpStatusCode status;
    private String message;

    public static ApiErrorModel badRequest(Exception e) {
        ApiErrorModel error = withMessageFrom(e);
        error.setStatus(HttpStatus.BAD_REQUEST);
        return error;
    }

    public static ApiErrorModel notFound(Exception e) {
        ApiErrorModel error = withMessageFrom(e);
        error.setStatus(HttpStatus.NOT_FOUND);
        return error;
    }

    public static ApiErrorModel conflict(Exception e) {
        ApiErrorModel error = withMessageFrom(e);
        error.setStatus(HttpStatus.CONFLICT);
        return error;
    }

    public ResponseEntity<ApiErrorModel> toResponse() {
        return new ResponseEntity<>(this, getStatus());
    }

    private static ApiErrorModel withMessageFrom(Exception e) {
        ApiErrorModel error = new ApiErrorModel();
        error.setMessage(e.getMessage());
        return error;
    }
}
