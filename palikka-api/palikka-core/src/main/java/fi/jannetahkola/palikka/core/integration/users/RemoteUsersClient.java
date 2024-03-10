package fi.jannetahkola.palikka.core.integration.users;

import com.nimbusds.jwt.JWTClaimsSet;
import fi.jannetahkola.palikka.core.auth.jwt.JwtService;
import fi.jannetahkola.palikka.core.auth.jwt.PalikkaJwtType;
import fi.jannetahkola.palikka.core.config.properties.RemoteUsersIntegrationProperties;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class RemoteUsersClient implements UsersClient {
    private final RemoteUsersIntegrationProperties properties;
    private final JwtService jwtService;
    private final Validator validator;

    public RemoteUsersClient(RemoteUsersIntegrationProperties properties, JwtService jwtService) {
        this.properties = properties;
        this.jwtService = jwtService;
        try (ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
            this.validator = validatorFactory.getValidator();
        }
    }

    @Override
    public User getUser(Integer userId) {
        RestTemplate restTemplate = new RestTemplate();
        URI uri = properties.getBaseUri().resolve("/users-api/users-api/users/" + userId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        jwtService.sign(new JWTClaimsSet.Builder(), PalikkaJwtType.SYSTEM).ifPresent(headers::setBearerAuth);

        HttpEntity<Void> httpEntity = new HttpEntity<>(null, headers);

        log.debug(">> GET {}", uri);

        try {

            ResponseEntity<User> exchange = restTemplate.exchange(uri, HttpMethod.GET, httpEntity, User.class);

            if (exchange.getStatusCode().is2xxSuccessful()) {
                User user = exchange.getBody();
                Set<ConstraintViolation<User>> violations = this.validator.validate(user);
                if (violations.isEmpty()) {
                    return user;
                }
                String formattedViolationMessage = violations.stream()
                        .map(violation -> String.format("%s: %s", violation.getPropertyPath(), violation.getMessage()))
                        .collect(Collectors.joining(", "));
                log.warn("Request 'GET {}' failed on invalid response. Status={}, response={}, violations={}",
                        uri, exchange.getStatusCode(), user, formattedViolationMessage);
                return null;
            }
            log.warn("Request 'GET {}' failed on invalid status. Status={}, response={}", uri, exchange.getStatusCode(), exchange.getBody());
        } catch (RestClientException e) {
            if (e instanceof HttpClientErrorException httpException) {
                log.warn("Request 'GET {}' failed on exception. Status={}, body={}",
                        uri, httpException.getStatusCode(), httpException.getResponseBodyAsString(), httpException);
            } else {
                log.warn("Request 'GET {}' failed on exception. Message={}", uri, e.getMessage(), e);
            }
        }
        return null;
    }
}
