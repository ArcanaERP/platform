package com.arcanaerp.platform.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.arcanaerp.platform.core.pagination.PageQuery;
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

    @Test
    void filtersUsersByTenantRoleAndActive() {
        UserView activeAdmin = userDirectory.registerUser(
            new RegisterUserCommand(
                "userdir03",
                "User Dir 03",
                "admin",
                "Administrator",
                "admin03@acme.com",
                "Admin 03"
            )
        );
        UserView activeClerk = userDirectory.registerUser(
            new RegisterUserCommand(
                "userdir03",
                "User Dir 03",
                "clerk",
                "Clerk",
                "clerk03@acme.com",
                "Clerk 03"
            )
        );
        UserView otherTenantUser = userDirectory.registerUser(
            new RegisterUserCommand(
                "userdir04",
                "User Dir 04",
                "admin",
                "Administrator",
                "admin04@acme.com",
                "Admin 04"
            )
        );
        userDirectory.updateUser(new UpdateUserCommand(activeClerk.id().toString(), "Clerk 03", false));

        var filtered = userDirectory.listUsers(new PageQuery(0, 10), "userdir03", "admin", true);

        assertThat(filtered.totalItems()).isEqualTo(1);
        assertThat(filtered.items()).extracting(UserView::id).containsExactly(activeAdmin.id());
        assertThat(filtered.items()).extracting(UserView::tenantCode).containsOnly("USERDIR03");
        assertThat(filtered.items()).extracting(UserView::roleCode).containsOnly("ADMIN");
        assertThat(filtered.items()).extracting(UserView::active).containsOnly(true);
        assertThat(filtered.items()).extracting(UserView::id)
            .doesNotContain(activeClerk.id(), otherTenantUser.id());
    }

    @Test
    void rejectsRoleFilterWithoutTenantCode() {
        assertThatThrownBy(() -> userDirectory.listUsers(new PageQuery(0, 10), null, "admin", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("tenantCode is required when roleCode is provided");
    }
}
