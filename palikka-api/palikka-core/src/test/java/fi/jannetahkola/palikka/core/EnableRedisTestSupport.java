package fi.jannetahkola.palikka.core;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Enables an embedded Redis server. Requires adding both Core test jar and the following test dependency, which is
 * versioned by the parent POM:
 * <pre>{@code
 * <dependency>
 *     <groupId>com.github.codemonstur</groupId>
 *     <artifactId>embedded-redis</artifactId>
 *     <scope>test</scope>
 * </dependency>
 * }</pre>
 * <br>
 * Tests should also start and stop the server in Jupiter lifecycle methods. Using post construct/pre destroy in the
 * embedded server itself causes issues in this project.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Documented
@Import(TestRedisConfig.class)
public @interface EnableRedisTestSupport {
}
