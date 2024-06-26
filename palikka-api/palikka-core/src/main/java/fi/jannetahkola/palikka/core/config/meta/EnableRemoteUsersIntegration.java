package fi.jannetahkola.palikka.core.config.meta;

import fi.jannetahkola.palikka.core.config.JwtConfig;
import fi.jannetahkola.palikka.core.config.UsersIntegrationConfig;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Documented
@Import({
        JwtConfig.class,
        UsersIntegrationConfig.class
})
public @interface EnableRemoteUsersIntegration {
}
