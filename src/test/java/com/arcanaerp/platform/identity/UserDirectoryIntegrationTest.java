package com.arcanaerp.platform.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class UserDirectoryIntegrationTest {

    @Autowired
    private UserDirectory userDirectory;

    @Test
    void readsUserById() {
        UserView created = userDirectory.registerUser(
            new RegisterUserCommand(
                "userdir01",
                "User Dir 01",
                "admin",
                "Administrator",
                "userdir01@acme.com",
                "User Dir 01"
            )
        );

        UserView user = userDirectory.userById(created.id().toString());

        assertThat(user.id()).isEqualTo(created.id());
        assertThat(user.tenantCode()).isEqualTo("USERDIR01");
        assertThat(user.roleCode()).isEqualTo("ADMIN");
        assertThat(user.email()).isEqualTo("userdir01@acme.com");
    }

    @Test
    void rejectsMissingUserById() {
        String missingUserId = "11111111-1111-1111-1111-111111111111";

        assertThatThrownBy(() -> userDirectory.userById(missingUserId))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessage("User not found: " + missingUserId);
    }

    @Test
    void rejectsInvalidUserId() {
        assertThatThrownBy(() -> userDirectory.userById("not-a-uuid"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("userId is invalid");
    }
}
