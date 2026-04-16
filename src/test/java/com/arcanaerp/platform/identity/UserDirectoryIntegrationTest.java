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
    void updatesUserDisplayNameAndActiveState() {
        UserView created = userDirectory.registerUser(
            new RegisterUserCommand(
                "userdir02",
                "User Dir 02",
                "admin",
                "Administrator",
                "userdir02@acme.com",
                "User Dir 02"
            )
        );

        UserView updated = userDirectory.updateUser(
            new UpdateUserCommand(created.id().toString(), "User Dir 02 Renamed", false)
        );

        assertThat(updated.id()).isEqualTo(created.id());
        assertThat(updated.displayName()).isEqualTo("User Dir 02 Renamed");
        assertThat(updated.active()).isFalse();
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

    @Test
    void rejectsMissingUserUpdate() {
        String missingUserId = "11111111-1111-1111-1111-111111111111";

        assertThatThrownBy(() -> userDirectory.updateUser(
            new UpdateUserCommand(missingUserId, "Missing User", false)
        ))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessage("User not found: " + missingUserId);
    }
}
