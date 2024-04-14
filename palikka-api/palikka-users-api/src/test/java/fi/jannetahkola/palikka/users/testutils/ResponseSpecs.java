package fi.jannetahkola.palikka.users.testutils;

import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.specification.ResponseSpecification;
import lombok.experimental.UtilityClass;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.equalTo;

@UtilityClass
public class ResponseSpecs {
    public static ResponseSpecification fullAuthenticationRequiredResponse() {
        return new ResponseSpecBuilder()
                .expectStatusCode(403)
                .expectBody("detail", equalTo("Full authentication is required to access this resource"))
                .expectHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE))
                .build();
    }

    public static ResponseSpecification accessDeniedResponse() {
        return new ResponseSpecBuilder()
                .expectStatusCode(403)
                .expectBody("detail", equalTo("Access Denied"))
                .expectHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE))
                .build();
    }
}
