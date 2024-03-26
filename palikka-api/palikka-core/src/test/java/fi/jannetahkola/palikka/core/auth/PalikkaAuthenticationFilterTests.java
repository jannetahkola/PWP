package fi.jannetahkola.palikka.core.auth;

import com.nimbusds.jwt.JWTClaimsSet;
import fi.jannetahkola.palikka.core.auth.jwt.JwtService;
import fi.jannetahkola.palikka.core.integration.users.Privilege;
import fi.jannetahkola.palikka.core.integration.users.Role;
import fi.jannetahkola.palikka.core.integration.users.User;
import fi.jannetahkola.palikka.core.integration.users.RemoteUsersClient;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class PalikkaAuthenticationFilterTests {

    @Mock
    JwtService jwtService;

    @Mock
    RemoteUsersClient usersClient;

    @SneakyThrows
    @Test
    void testNoAuthorizationHeaderInRequest() {
        var filter = new PalikkaAuthenticationFilter(jwtService, usersClient);

        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(req, res, chain);
        verify(jwtService, times(0)).parse(any());
    }

    @SneakyThrows
    @Test
    void testNoTokenValueInRequest() {
        var filter = new PalikkaAuthenticationFilter(jwtService, usersClient);

        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        req.addHeader(HttpHeaders.AUTHORIZATION, "Bearer ");
        filter.doFilterInternal(req, res, chain);
        verify(jwtService, times(0)).parse(any());
    }

    @SneakyThrows
    @Test
    void testInvalidTokenValueInRequest() {
        var filter = new PalikkaAuthenticationFilter(jwtService, usersClient);

        when(jwtService.parse(any())).thenReturn(Optional.empty());

        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        req.addHeader(HttpHeaders.AUTHORIZATION, "Bearer invalid-token");
        filter.doFilterInternal(req, res, chain);

        verify(jwtService, times(1)).parse(any());
        verify(usersClient, times(0)).getUser(any());
    }

    @SneakyThrows
    @Test
    void testValidTokenButUnknownSubjectInRequest() {
        var filter = new PalikkaAuthenticationFilter(jwtService, usersClient);

        when(jwtService.parse(any())).thenReturn(Optional.of(new JWTClaimsSet.Builder().subject("1").build()));
        when(usersClient.getUser(any())).thenReturn(null);

        try (MockedStatic<SecurityContextHolder> securityContextMock = Mockito.mockStatic(SecurityContextHolder.class)) {
            securityContextMock.when(SecurityContextHolder::getContext).thenReturn(new SecurityContextImpl());

            MockHttpServletRequest req = new MockHttpServletRequest();
            MockHttpServletResponse res = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            req.addHeader(HttpHeaders.AUTHORIZATION, "Bearer valid-token");
            filter.doFilterInternal(req, res, chain);

            verify(jwtService, times(1)).parse(any());
            verify(usersClient, times(1)).getUser(any());
            securityContextMock.verify(SecurityContextHolder::getContext, times(0));
        }
    }

    @SneakyThrows
    @Test
    void testValidTokenButInactiveUser() {
        var filter = new PalikkaAuthenticationFilter(jwtService, usersClient);

        when(jwtService.parse(any())).thenReturn(Optional.of(new JWTClaimsSet.Builder().subject("1").build()));
        when(usersClient.getUser(any())).thenReturn(User.builder().id(1).username("user").roles(Set.of("USERS_ALL")).active(false).build());

        try (MockedStatic<SecurityContextHolder> securityContextMock = Mockito.mockStatic(SecurityContextHolder.class)) {
            securityContextMock.when(SecurityContextHolder::getContext).thenReturn(new SecurityContextImpl());

            MockHttpServletRequest req = new MockHttpServletRequest();
            MockHttpServletResponse res = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            req.addHeader(HttpHeaders.AUTHORIZATION, "Bearer valid-token");
            filter.doFilterInternal(req, res, chain);

            verify(jwtService, times(1)).parse(any());
            verify(usersClient, times(1)).getUser(any());
            securityContextMock.verify(SecurityContextHolder::getContext, times(0));
        }
    }

    @SneakyThrows
    @Test
    void testValidTokenWithValidUser() {
        var filter = new PalikkaAuthenticationFilter(jwtService, usersClient);

        when(jwtService.parse(any())).thenReturn(Optional.of(new JWTClaimsSet.Builder().subject("1").build()));
        when(usersClient.getUser(any())).thenReturn(
                User.builder()
                        .id(1)
                        .username("user")
                        .roles(Set.of("ROLE_ADMIN"))
                        .active(true)
                        .root(true)
                        .build()
        );
        when(usersClient.getUserRoles(any())).thenReturn(
                List.of(
                        Role.builder()
                                .id(1)
                                .name("ROLE_ADMIN")
                                .privileges(
                                        Set.of(
                                                Privilege.builder()
                                                        .id(1)
                                                        .category("COMMAND")
                                                        .name("weather")
                                                        .build()))
                                .build())
        );

        try (MockedStatic<SecurityContextHolder> securityContextMock = Mockito.mockStatic(SecurityContextHolder.class)) {
            securityContextMock.when(SecurityContextHolder::getContext).thenReturn(new SecurityContextImpl());

            MockHttpServletRequest req = new MockHttpServletRequest();
            MockHttpServletResponse res = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            req.addHeader(HttpHeaders.AUTHORIZATION, "Bearer valid-token");
            filter.doFilterInternal(req, res, chain);

            verify(jwtService, times(1)).parse(any());
            verify(usersClient, times(1)).getUser(any());
            securityContextMock.verify(SecurityContextHolder::getContext, times(1));

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            assertThat(authentication.isAuthenticated()).isTrue();
            assertThat(authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList())
                    .contains("ROLE_ADMIN", "COMMAND_weather");
        }
    }

    @SneakyThrows
    @Test
    void testValidTokenWithSystemUser() {
        var filter = new PalikkaAuthenticationFilter(jwtService, usersClient);

        when(jwtService.parse(any())).thenReturn(Optional.of(new JWTClaimsSet.Builder().build()));

        try (MockedStatic<SecurityContextHolder> securityContextMock = Mockito.mockStatic(SecurityContextHolder.class)) {
            securityContextMock.when(SecurityContextHolder::getContext).thenReturn(new SecurityContextImpl());

            MockHttpServletRequest req = new MockHttpServletRequest();
            MockHttpServletResponse res = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            req.addHeader(HttpHeaders.AUTHORIZATION, "Bearer valid-token");
            filter.doFilterInternal(req, res, chain);

            verify(jwtService, times(1)).parse(any());
            verify(usersClient, times(0)).getUser(any());
            securityContextMock.verify(SecurityContextHolder::getContext, times(1));

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            assertThat(authentication.isAuthenticated()).isTrue();
        }
    }
}
