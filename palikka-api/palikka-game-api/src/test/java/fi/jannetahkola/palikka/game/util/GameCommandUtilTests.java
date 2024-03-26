package fi.jannetahkola.palikka.game.util;

import fi.jannetahkola.palikka.core.auth.PalikkaPrincipal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class GameCommandUtilTests {
    @ParameterizedTest
    @MethodSource("normalizeCommandParams")
    void testNormalizeCommand(String rawCommand, String expectedCommand) {
        assertThat(GameCommandUtil.normalizeCommand(rawCommand)).isEqualTo(expectedCommand);
    }

    @ParameterizedTest
    @MethodSource("authorizeCommandParams")
    void testAuthorizeCommand(List<String> authorities, String normalizedCommand, boolean expectedResult) {
        List<SimpleGrantedAuthority> mappedAuthorities =
                authorities.stream().map(SimpleGrantedAuthority::new).toList();
        PalikkaPrincipal principal = new PalikkaPrincipal(1, "mock-user");
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, mappedAuthorities);
        assertThat(GameCommandUtil.authorizeCommand(authentication, normalizedCommand)).isEqualTo(expectedResult);
    }

    static Stream<Arguments> normalizeCommandParams() {
        return Stream.of(
                Arguments.of("/weather clear", "weather"),
                Arguments.of("weather clear", "weather"),
                Arguments.of("/weather", "weather"),
                Arguments.of("weather", "weather"),
                Arguments.of("wEather clear", "weather")
        );
    }

    static Stream<Arguments> authorizeCommandParams() {
        return Stream.of(
                Arguments.of(List.of("ROLE_USER", "COMMAND_weather"), "weather", true),
                Arguments.of(List.of("COMMAND_op", "COMMAND_weather"), "weather", true),
                Arguments.of(List.of(), "weather", false),
                Arguments.of(List.of("ROLE_USER"), "weather", false),
                Arguments.of(List.of("COMMAND_op"), "weather", false)
        );
    }
}
