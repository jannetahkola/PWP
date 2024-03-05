package fi.jannetahkola.palikka.users.api.auth;

import fi.jannetahkola.palikka.core.auth.jwt.JwtService;
import fi.jannetahkola.palikka.users.data.auth.TokenEntity;
import fi.jannetahkola.palikka.users.data.auth.TokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@RestController
@RequestMapping("/auth/logout")
@RequiredArgsConstructor
public class LogoutController {
    private final JwtService jwtService;
    private final TokenRepository tokenRepository;

    @PostMapping
    public void logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        jwtService.parse(authorizationHeader.split("Bearer ")[1]) // Auth filter has validated the format already
                .ifPresent(claims -> {
                    log.debug("Logging out user id '{}'", claims.getSubject());
                    TokenEntity tokenEntity = new TokenEntity();
                    tokenEntity.setTokenId(claims.getJWTID());
                    tokenEntity.setAddedOn(LocalDateTime.ofInstant(Instant.now(), ZoneId.of("UTC")));
                    tokenRepository.save(tokenEntity);
                    log.debug("Logged out user id '{}'", claims.getSubject());
                });
    }
}
