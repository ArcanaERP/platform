package com.arcanaerp.platform.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.arcanaerp.platform.core.pagination.PageQuery;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class RoleDirectoryIntegrationTest {

    @Autowired
    private UserDirectory userDirectory;

    @Autowired
    private RoleDirectory roleDirectory;

    @Test
    void listsRolesByTenant() {
        userDirectory.registerUser(
            new RegisterUserCommand("rolten01", "Role Tenant 01", "admin", "Administrator", "role01@acme.com", "Role 01")
        );
        userDirectory.registerUser(
            new RegisterUserCommand("rolten01", "Role Tenant 01", "analyst", "Analyst", "role02@acme.com", "Role 02")
        );

        var roles = roleDirectory.listRoles("rolten01", new PageQuery(0, 10));

        assertThat(roles.totalItems()).isEqualTo(2);
        assertThat(roles.items()).extracting(RoleView::code).containsExactly("ADMIN", "ANALYST");
        assertThat(roles.items()).extracting(RoleView::tenantCode).containsOnly("ROLTEN01");
    }

    @Test
    void rejectsMissingTenant() {
        assertThatThrownBy(() -> roleDirectory.listRoles("missing-role-tenant", new PageQuery(0, 10)))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessage("Tenant not found: MISSING-ROLE-TENANT");
    }
}
