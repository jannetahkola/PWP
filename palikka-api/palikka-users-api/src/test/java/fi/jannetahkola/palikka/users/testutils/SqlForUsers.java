package fi.jannetahkola.palikka.users.testutils;

import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;
import org.springframework.test.context.jdbc.SqlMergeMode;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Documented
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE) // Allow additional @Sql annotations
// Insert without ids or else PK generation doesn't take them into account -> POST fails on PK constraint violation.
// These should be executed first so ids should be sequential.
// See also https://stackoverflow.com/questions/76206121/integration-test-on-h2-errors-on-unique-index-violation-stale-id-sequence-afte
@SqlGroup(value = {
        @Sql(
                statements = {
                        "insert into palikka_user (username, password, salt, active) values ('mock-user', 'mock-pass', 'mock-salt', true)",
                        "insert into palikka_role (name, description) values ('ROLE_ADMIN', 'Access to everything')",
                        "insert into palikka_user_role (user_id, role_id) values (1, 1)"},
                executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
        ),
        @Sql(
                statements = {
                        "insert into palikka_user (username, password, salt, active) values ('mock-user-2', 'mock-pass', 'mock-salt', true)",
                        "insert into palikka_role (name, description) values ('ROLE_USER', 'Access to limited functionality and self endpoints')",
                        "insert into palikka_user_role (user_id, role_id) values (2, 2)"},
                executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
        ),
})
public @interface SqlForUsers {
}
