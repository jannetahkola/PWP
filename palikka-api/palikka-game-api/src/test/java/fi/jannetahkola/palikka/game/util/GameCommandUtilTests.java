package fi.jannetahkola.palikka.game.util;

import fi.jannetahkola.palikka.core.auth.PalikkaPrincipal;
import fi.jannetahkola.palikka.core.integration.users.Privilege;
import fi.jannetahkola.palikka.core.integration.users.Role;
import fi.jannetahkola.palikka.core.integration.users.User;
import fi.jannetahkola.palikka.core.integration.users.UsersClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

class GameCommandUtilTests {
    UsersClient mockUsersClient;

    @BeforeEach
    void beforeEach() {
        mockUsersClient = Mockito.mock(UsersClient.class);
        Mockito.when(mockUsersClient.getUser(any())).thenReturn(
                User.builder()
                        .id(1)
                        .active(true)
                        .root(true)
                        .username("user")
                        .build()
        );
    }

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
        Mockito.when(mockUsersClient.getUserRoles(any())).thenReturn(List.of(
                Role.builder()
                        .id(1)
                        .name("ROLE_USER")
                        .privileges(
                                authorities.stream().map(a -> {
                                    String[] s = a.split("_");
                                    return Privilege.builder()
                                            .domain(s[0])
                                            .name(s[1])
                                            .build();
                                })
                                .collect(Collectors.toSet())
                        )
                        .build()
        ));
        boolean authorized = GameCommandUtil.authorizeCommand(mockUsersClient, authentication, normalizedCommand);
        assertThat(authorized).isEqualTo(expectedResult);
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
                Arguments.of(List.of("COMMAND_weather"), "weather", true),
                Arguments.of(List.of("COMMAND_op", "COMMAND_weather"), "weather", true),
                Arguments.of(List.of(), "weather", false),
                Arguments.of(List.of("COMMAND_op"), "weather", false)
        );
    }
}
