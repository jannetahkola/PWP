package fi.jannetahkola.palikka.users.api.role;

import fi.jannetahkola.palikka.core.integration.users.Privilege;
import fi.jannetahkola.palikka.core.integration.users.Role;
import fi.jannetahkola.palikka.users.api.role.model.RolePrivilegePostModel;
import fi.jannetahkola.palikka.users.data.privilege.PrivilegeEntity;
import fi.jannetahkola.palikka.users.data.privilege.PrivilegeRepository;
import fi.jannetahkola.palikka.users.testutils.IntegrationTest;
import io.restassured.RestAssured;
import io.restassured.http.Header;
import lombok.SneakyThrows;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.client.Traverson;
import org.springframework.hateoas.server.core.TypeReferences;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.hateoas.client.Hop.rel;

class RolePrivilegeControllerIT extends IntegrationTest {
    @Test
    void givenGetRolePrivilegesOptionsRequest_thenAllowedMethodsReturned() {
        given()
                .header(newAdminToken())
                .options("/roles/2/privileges")
                .then().assertThat()
                .statusCode(200)
                .header(HttpHeaders.ALLOW, containsString("GET"))
                .header(HttpHeaders.ALLOW, containsString("POST"))
                .header(HttpHeaders.ALLOW, not(containsString("PUT")))
                .header(HttpHeaders.ALLOW, not(containsString("PATCH")));
    }

    @Test
    void givenGetRolePrivilegesRequest_whenAcceptHalFormsHeaderGiven_thenResponseContainsTemplates() {
        given()
                .header(newAdminToken())
                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                .get("/roles/1/privileges")
                .then().assertThat()
                .statusCode(200)
                .body("_embedded.privileges[0]._links.self.href", endsWith("/users-api/roles/1/privileges/1"))
                .body("_embedded.privileges[0]._templates.default.method", equalTo("DELETE"))
                .body("_templates.default.method", equalTo("POST"))
                .body("_templates.default.properties[0].name", equalTo("privilege_id"))
                .body("_templates.default.properties[0].required", equalTo(true))
                .body("_templates.default.properties[0].type", equalTo("number"))
                .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaTypes.HAL_FORMS_JSON_VALUE));
    }

    @Test
    void givenGetSingleRolePrivilegeRequest_whenAcceptHalFormsHeaderGiven_thenResponseContainsTemplates() {
        given()
                .header(newAdminToken())
                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                .get("/roles/1/privileges/1")
                .then().assertThat()
                .statusCode(200)
                .body("id", equalTo(1))
                .body("_links.self.href", endsWith("/users-api/roles/1/privileges/1"))
                .body("_templates.default.method", equalTo("DELETE"))
                .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaTypes.HAL_FORMS_JSON_VALUE));
    }

    @Test
    void givenGetSingleRolePrivilegeRequest_thenOkResponse() {
        given()
                .header(newAdminToken())
                .get("/roles/1/privileges/1")
                .then().assertThat()
                .statusCode(200)
                .body("id", equalTo(1))
                .body("_links.self.href", endsWith("/users-api/roles/1/privileges/1"))
                .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaTypes.HAL_JSON_VALUE));
    }

    @Test
    void givenGetSingleRolePrivilegeRequest_whenRoleNotFound_thenNotFoundResponse() {
        given()
                .header(newAdminToken())
                .get("/roles/999/privileges/1")
                .then().assertThat()
                .statusCode(404)
                .body("detail", equalTo("Role with id '999' not found"))
                .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
    }

    @Test
    void givenGetSingleRolePrivilegeRequest_whenPrivilegeNotAssociatedWithRole_thenNotFoundResponse() {
        given()
                .header(newAdminToken())
                .get("/roles/1/privileges/999")
                .then().assertThat()
                .statusCode(404)
                .body("detail", equalTo("Privilege with id '999' not found"))
                .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
    }

    @Test
    void givenPostRolePrivilegesRequest_thenAssociationCreated_andCreatedResponse(@Autowired PrivilegeRepository privilegeRepository) {
        Integer privilegeId = privilegeRepository.findByName("weather").orElseThrow().getId();
        RolePrivilegePostModel postModel = RolePrivilegePostModel.builder().privilegeId(privilegeId).build();
        given()
                .header(newAdminToken())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(postModel)
                .post("/roles/2/privileges")
                .then().assertThat()
                .statusCode(201)
                .body("_embedded.privileges", hasSize(4))
                .body("_links.self.href", endsWith("/roles/2/privileges"))
                .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaTypes.HAL_JSON_VALUE));

        URI baseUri = URI.create(RestAssured.baseURI + ":" + RestAssured.port).resolve("/users-api/roles");
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setBearerAuth(testTokenGenerator.generateToken(USER_ID_ADMIN));

        Traverson traverson = new Traverson(baseUri, MediaTypes.HAL_JSON);
        EntityModel<Role> roleCollectionModel = traverson
                .follow(rel("role").withParameter("id", 2))
                .withHeaders(httpHeaders)
                .toObject(new TypeReferences.EntityModelType<>() {});
        assertThat(roleCollectionModel).isNotNull();
        assertThat(roleCollectionModel.getContent()).isNotNull();
        Set<Privilege> privileges = roleCollectionModel.getContent().getPrivileges();
        assertThat(privileges.stream().anyMatch(privilege -> privilege.getId().equals(privilegeId))).isTrue();
    }

    @Test
    void givenPostRolePrivilegesRequest_whenAssociationAlreadyExists_thenCreatedResponse(@Autowired PrivilegeRepository privilegeRepository) {
        Header authHeader = newAdminToken();
        given()
                .header(authHeader)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .get("/roles/2/privileges")
                .then().assertThat()
                .statusCode(200)
                .body("_embedded.privileges", hasSize(4));
        Integer privilegeId = privilegeRepository.findByName("help").orElseThrow().getId();
        RolePrivilegePostModel postModel = RolePrivilegePostModel.builder().privilegeId(privilegeId).build();
        given()
                .header(authHeader)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(postModel)
                .post("/roles/2/privileges")
                .then().assertThat()
                .statusCode(201)
                .body("_embedded.privileges", hasSize(4))
                .body("_links.self.href", endsWith("/roles/2/privileges"))
                .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaTypes.HAL_JSON_VALUE));
    }

    @Test
    void givenPostRolePrivilegesRequest_whenRoleNotFound_thenNotFoundResponse(@Autowired PrivilegeRepository privilegeRepository) {
        Integer privilegeId = privilegeRepository.findByName("weather").orElseThrow().getId();
        RolePrivilegePostModel postModel = RolePrivilegePostModel.builder().privilegeId(privilegeId).build();
        given()
                .header(newAdminToken())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(postModel)
                .post("/roles/999/privileges")
                .then().assertThat()
                .statusCode(404)
                .body("detail", equalTo("Role with id '999' not found"))
                .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
    }

    @Test
    void givenPostRolePrivilegesRequest_whenPrivilegeNotFound_thenNotFoundResponse() {
        RolePrivilegePostModel postModel = RolePrivilegePostModel.builder().privilegeId(999).build();
        given()
                .header(newAdminToken())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(postModel)
                .post("/roles/1/privileges")
                .then().assertThat()
                .statusCode(404)
                .body("detail", equalTo("Privilege with id '999' not found"))
                .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
    }

    @ParameterizedTest
    @MethodSource("invalidPostRolePrivilegesParameters")
    void givenPostRolePrivilegesRequest_whenParametersInvalid_thenBadRequestResponse(JSONObject json,
                                                                                     String expectedMessageSubstring) {
        given()
                .header(newAdminToken())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(json.toString())
                .post("/roles/2/privileges")
                .then().assertThat()
                .statusCode(400)
                .body("detail", containsString(expectedMessageSubstring))
                .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE));;
    }

    @Test
    void givenDeleteRolePrivilegesRequest_thenAssociationDeleted_andNoContentResponse(@Autowired PrivilegeRepository privilegeRepository) {
        final int roleId = 2;
        final List<PrivilegeEntity> allAssociatedPrivileges = privilegeRepository.findAllByRoleId(roleId);
        final Integer associatedPrivilegeId = allAssociatedPrivileges.stream().findAny().orElseThrow().getId();
        final Header authHeader = newAdminToken();
        given()
                .header(authHeader)
                .delete("/roles/" + roleId + "/privileges/" + associatedPrivilegeId)
                .then().assertThat()
                .statusCode(204);
        given()
                .header(authHeader)
                .get("/roles/" + roleId + "/privileges")
                .then().assertThat()
                .statusCode(200)
                .body("_embedded.privileges", hasSize(allAssociatedPrivileges.size() - 1));
    }

    @Test
    void givenDeleteRolePrivilegesRequest_whenPrivilegeNotAssociatedWithRole_thenNoContentResponse(@Autowired PrivilegeRepository privilegeRepository) {
        final int roleId = 2;
        final int originalPrivilegesCount = privilegeRepository.findAllByRoleId(roleId).size();
        final Integer nonAssociatedPrivilegeId = privilegeRepository.findByName("op").orElseThrow().getId();
        final Header authHeader = newAdminToken();
        given()
                .header(authHeader)
                .delete("/roles/" + roleId + "/privileges/" + nonAssociatedPrivilegeId)
                .then().assertThat()
                .statusCode(204);
        given()
                .header(authHeader)
                .get("/roles/" + roleId + "/privileges")
                .then().assertThat()
                .statusCode(200)
                .body("_embedded.privileges", hasSize(originalPrivilegesCount));
    }

    @Test
    void givenDeleteRolePrivilegesRequest_whenRoleNotFound_thenNotFoundResponse() {
        given()
                .header(newAdminToken())
                .delete("/roles/999/privileges/1")
                .then().assertThat()
                .statusCode(404)
                .body("detail", equalTo("Role with id '999' not found"))
                .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
    }

    @SneakyThrows
    static Stream<Arguments> invalidPostRolePrivilegesParameters() {
        return Stream.of(
                Arguments.of(
                        Named.of(
                                "Empty privilege id",
                                new JSONObject().put(
                                        "privilege_id",
                                        "")),
                        "privilegeId: must not be null"
                ),
                Arguments.of(
                        Named.of(
                                "Blank privilege id",
                                new JSONObject().put(
                                        "privilege_id",
                                        " ")),
                        "privilegeId: must not be null"
                ),
                Arguments.of(
                        Named.of(
                                "No privilege id",
                                new JSONObject()),
                        "privilegeId: must not be null"
                ),
                Arguments.of(
                        Named.of(
                                "Invalid privilege id",
                                new JSONObject().put("privilege_id", "a")),
                        "Cannot deserialize value of type"
                )
        );
    }
}
