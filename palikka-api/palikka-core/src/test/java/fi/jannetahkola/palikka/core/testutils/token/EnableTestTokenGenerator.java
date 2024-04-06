package fi.jannetahkola.palikka.core.testutils.token;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Documented
@Import(TestTokenGeneratorConfig.class)
public @interface EnableTestTokenGenerator {
}
