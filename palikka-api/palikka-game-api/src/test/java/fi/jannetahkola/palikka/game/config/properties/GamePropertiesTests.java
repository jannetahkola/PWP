package fi.jannetahkola.palikka.game.config.properties;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Path;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class GamePropertiesTests {
    static final String VALID_FILENAME = "test.jar";
    static final String VALID_PATH = "/test";
    static final String VALID_START_COMMAND = "java -jar test.jar";

    static final Validator VALIDATOR;

    static {
        try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
            VALIDATOR = validatorFactory.getValidator();
        }
    }

    @Test
    void testValidFileNameValues() {
        GameProperties.FileProperties fileProperties = new GameProperties.FileProperties();
        fileProperties.setName(VALID_FILENAME);
        fileProperties.setPath(VALID_PATH);
        fileProperties.setStartCommand(VALID_START_COMMAND);

        Set<ConstraintViolation<GameProperties.FileProperties>> violations = VALIDATOR.validate(fileProperties);
        assertThat(violations).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("invalidFileNameArgs")
    void testInvalidFileNameValues(String fileNameValue, String expectedErrorMessagePrefix) {
        GameProperties.FileProperties fileProperties = new GameProperties.FileProperties();
        fileProperties.setName(fileNameValue);
        fileProperties.setPath(VALID_PATH);
        fileProperties.setStartCommand(VALID_START_COMMAND);

        Set<ConstraintViolation<GameProperties.FileProperties>> violations = VALIDATOR.validate(fileProperties);
        assertThat(violations).isNotEmpty();

        Optional<Path> violationPropertyPathMaybe = violations.stream()
                .filter(violation -> violation.getMessage().startsWith(expectedErrorMessagePrefix))
                .map(ConstraintViolation::getPropertyPath)
                .findAny();

        assertThat(violationPropertyPathMaybe)
                .isPresent().get()
                .hasToString("name");
    }

    @ParameterizedTest
    @MethodSource("validStartCommandArgs")
    void testValidStartCommandValues(String startCommandValue) {
        GameProperties.FileProperties fileProperties = new GameProperties.FileProperties();
        fileProperties.setName(VALID_FILENAME);
        fileProperties.setPath(VALID_PATH);
        fileProperties.setStartCommand(startCommandValue);

        Set<ConstraintViolation<GameProperties.FileProperties>> violations = VALIDATOR.validate(fileProperties);
        assertThat(violations).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("invalidStartCommandArgs")
    void testInvalidStartCommandValues(String startCommandValue, String expectedErrorMessagePrefix) {
        GameProperties.FileProperties fileProperties = new GameProperties.FileProperties();
        fileProperties.setName(VALID_FILENAME);
        fileProperties.setPath(VALID_PATH);
        fileProperties.setStartCommand(startCommandValue);

        Set<ConstraintViolation<GameProperties.FileProperties>> violations = VALIDATOR.validate(fileProperties);
        assertThat(violations).isNotEmpty();

        Optional<Path> violationPropertyPathMaybe = violations.stream()
                .filter(violation -> violation.getMessage().startsWith(expectedErrorMessagePrefix))
                .map(ConstraintViolation::getPropertyPath)
                .findAny();

        assertThat(violationPropertyPathMaybe)
                .isPresent().get()
                .hasToString("startCommand");
    }

    static Stream<Arguments> invalidFileNameArgs() {
        return Stream.of(
                Arguments.of("", "must not be blank"),
                Arguments.of(" ", "must not be blank"),
                Arguments.of("test", "must match"),
                Arguments.of("test.ja", "must match"),
                Arguments.of("test.jar ", "must match"),
                Arguments.of("test.jar\n", "must match")
        );
    }

    static Stream<Arguments> validStartCommandArgs() {
        return Stream.of(
                Arguments.of("java -jar test.jar"),
                Arguments.of("java -jar -Xmx2048 test.jar")
        );
    }

    static Stream<Arguments> invalidStartCommandArgs() {
        return Stream.of(
                Arguments.of("", "must not be blank"),
                Arguments.of(" ", "must not be blank"),
                Arguments.of("test", "must match"),
                Arguments.of("java -ja", "must match"),
                Arguments.of("java -jar test.ja", "must match"),
                Arguments.of("\njava -jar test.jar", "must match"),
                Arguments.of(" java -jar test.jar", "must match")
        );
    }
}
