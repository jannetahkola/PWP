package fi.jannetahkola.palikka.core.auth;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWTClaimsSet;
import fi.jannetahkola.palikka.core.auth.authenticator.JwtAuthenticationProvider;
import fi.jannetahkola.palikka.core.auth.jwt.JwtService;
import fi.jannetahkola.palikka.core.auth.data.RevokedTokenRepository;
import fi.jannetahkola.palikka.core.auth.jwt.PalikkaJwtType;
import fi.jannetahkola.palikka.core.auth.jwt.VerifiedJwt;
import fi.jannetahkola.palikka.core.integration.users.Privilege;
import fi.jannetahkola.palikka.core.integration.users.Role;
import fi.jannetahkola.palikka.core.integration.users.User;
import fi.jannetahkola.palikka.core.integration.users.RemoteUsersClient;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
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

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "logging.level.fi.jannetahkola.palikka.core=debug"
        })
class PalikkaAuthenticationFilterTests {
    private static final VerifiedJwt USER_JWT = VerifiedJwt.builder()
            .claims(new JWTClaimsSet.Builder().subject(String.valueOf(1)).build())
            .header(new JWSHeader.Builder(JWSAlgorithm.RS512).build())
            .type(PalikkaJwtType.USER)
            .token("")
            .build();

    private static final VerifiedJwt SYSTEM_JWT = VerifiedJwt.builder()
            .claims(new JWTClaimsSet.Builder().subject("sys").build())
            .header(new JWSHeader.Builder(JWSAlgorithm.RS512).build())
            .type(PalikkaJwtType.SYSTEM)
            .token("")
            .build();

    @Mock
    JwtService jwtService;

    @Mock
    RemoteUsersClient usersClient;

    @Mock
    RevokedTokenRepository revokedTokenRepository;

    PalikkaAuthenticationFilter filter;

    @BeforeEach
    void beforeEach() {
        when(jwtService.consumesTokenOfType(any())).thenReturn(true);
        JwtAuthenticationProvider jwtAuthenticationProvider = new JwtAuthenticationProvider(jwtService, usersClient, revokedTokenRepository);
        filter = new PalikkaAuthenticationFilter(jwtAuthenticationProvider);
    }

    @SneakyThrows
    @Test
    void testNoAuthorizationHeaderInRequest() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(req, res, chain);
        verify(jwtService, times(0)).parse(any());
    }

    @SneakyThrows
    @Test
    void testNoTokenValueInRequest() {
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
        when(jwtService.parse(any())).thenReturn(Optional.of(USER_JWT));
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
        when(jwtService.parse(any())).thenReturn(Optional.of(USER_JWT));
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
    void testRevokedToken() {
        when(jwtService.parse(any())).thenReturn(Optional.of(USER_JWT));
        when(revokedTokenRepository.existsById(any())).thenReturn(true);

        try (MockedStatic<SecurityContextHolder> securityContextMock = Mockito.mockStatic(SecurityContextHolder.class)) {
            securityContextMock.when(SecurityContextHolder::getContext).thenReturn(new SecurityContextImpl());

            MockHttpServletRequest req = new MockHttpServletRequest();
            MockHttpServletResponse res = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            req.addHeader(HttpHeaders.AUTHORIZATION, "Bearer valid-token");
            filter.doFilterInternal(req, res, chain);

            verify(jwtService, times(1)).parse(any());
            verify(usersClient, times(0)).getUser(any());
            securityContextMock.verify(SecurityContextHolder::getContext, times(0));

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            assertThat(authentication).isNull();
        }
    }

    @SneakyThrows
    @Test
    void testValidTokenWithValidUser() {
        when(jwtService.parse(any())).thenReturn(Optional.of(USER_JWT));
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
                                                        .domain("COMMAND")
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
        when(jwtService.parse(any())).thenReturn(Optional.of(SYSTEM_JWT));

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
