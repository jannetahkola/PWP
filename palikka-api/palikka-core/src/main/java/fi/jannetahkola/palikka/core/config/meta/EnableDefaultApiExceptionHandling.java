package fi.jannetahkola.palikka.core.config.meta;

import fi.jannetahkola.palikka.core.config.DefaultApiExceptionHandlingConfig;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Documented
@Import(DefaultApiExceptionHandlingConfig.class)
public @interface EnableDefaultApiExceptionHandling {
}
