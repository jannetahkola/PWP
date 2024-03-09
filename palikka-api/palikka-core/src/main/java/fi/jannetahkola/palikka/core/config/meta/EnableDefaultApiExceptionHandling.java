package fi.jannetahkola.palikka.core.config.meta;

import fi.jannetahkola.palikka.core.api.exception.DefaultApiExceptionHandler;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Documented
@Import(DefaultApiExceptionHandler.class)
public @interface EnableDefaultApiExceptionHandling {
}
