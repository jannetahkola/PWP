package fi.jannetahkola.palikka.core.api.exception;

import fi.jannetahkola.palikka.core.api.exception.model.ErrorModel;
import org.springframework.http.ResponseEntity;

public class ExceptionUtil {
    ExceptionUtil() {
        // Util
    }

    public static <T extends ErrorModel> ResponseEntity<T> errorResponseOf(T model) {
        return new ResponseEntity<>(model, model.getStatusCode());
    }
}
