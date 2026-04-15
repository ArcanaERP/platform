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
    void registersAndListsRolesByTenant() {
        roleDirectory.registerRole(
            new RegisterRoleCommand("rolten01", "Role Tenant 01", "admin", "Administrator")
        );
        roleDirectory.registerRole(
            new RegisterRoleCommand("rolten01", "Role Tenant 01", "analyst", "Analyst")
        );

        var roles = roleDirectory.listRoles("rolten01", new PageQuery(0, 10));

        assertThat(roles.totalItems()).isEqualTo(2);
        assertThat(roles.items()).extracting(RoleView::code).containsExactly("ADMIN", "ANALYST");
        assertThat(roles.items()).extracting(RoleView::tenantCode).containsOnly("ROLTEN01");
    }

    @Test
    void listsRolesByTenant() {
        userDirectory.registerUser(
            new RegisterUserCommand("rolten02", "Role Tenant 02", "admin", "Administrator", "role01@acme.com", "Role 01")
        );
        userDirectory.registerUser(
            new RegisterUserCommand("rolten02", "Role Tenant 02", "analyst", "Analyst", "role02@acme.com", "Role 02")
        );

        var roles = roleDirectory.listRoles("rolten02", new PageQuery(0, 10));

        assertThat(roles.totalItems()).isEqualTo(2);
        assertThat(roles.items()).extracting(RoleView::code).containsExactly("ADMIN", "ANALYST");
        assertThat(roles.items()).extracting(RoleView::tenantCode).containsOnly("ROLTEN02");
    }

    @Test
    void readsRoleByTenantAndCode() {
        roleDirectory.registerRole(
            new RegisterRoleCommand("rolten04", "Role Tenant 04", "admin", "Administrator")
        );

        RoleView role = roleDirectory.roleByCode("rolten04", "admin");

        assertThat(role.tenantCode()).isEqualTo("ROLTEN04");
        assertThat(role.code()).isEqualTo("ADMIN");
        assertThat(role.name()).isEqualTo("Administrator");
    }

    @Test
    void updatesRoleName() {
        roleDirectory.registerRole(
            new RegisterRoleCommand("rolten06", "Role Tenant 06", "admin", "Administrator")
        );

        RoleView updated = roleDirectory.updateRole(
            new UpdateRoleCommand("rolten06", "admin", "Administrator Renamed")
        );

        assertThat(updated.tenantCode()).isEqualTo("ROLTEN06");
        assertThat(updated.code()).isEqualTo("ADMIN");
        assertThat(updated.name()).isEqualTo("Administrator Renamed");
    }

    @Test
    void rejectsDuplicateRoleCodeInTenant() {
        roleDirectory.registerRole(
            new RegisterRoleCommand("rolten03", "Role Tenant 03", "admin", "Administrator")
        );

        assertThatThrownBy(() ->
            roleDirectory.registerRole(
                new RegisterRoleCommand("rolten03", "Role Tenant 03", "ADMIN", "Administrator Copy")
            )
        )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Role code already exists in tenant: ADMIN");
    }

    @Test
    void rejectsMissingTenant() {
        assertThatThrownBy(() -> roleDirectory.listRoles("missing-role-tenant", new PageQuery(0, 10)))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessage("Tenant not found: MISSING-ROLE-TENANT");
    }

    @Test
    void rejectsMissingRoleByTenantAndCode() {
        roleDirectory.registerRole(
            new RegisterRoleCommand("rolten05", "Role Tenant 05", "admin", "Administrator")
        );

        assertThatThrownBy(() -> roleDirectory.roleByCode("rolten05", "missing-role"))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessage("Role not found for tenant/code: ROLTEN05/MISSING-ROLE");
    }

    @Test
    void rejectsMissingRoleUpdateByTenantAndCode() {
        roleDirectory.registerRole(
            new RegisterRoleCommand("rolten07", "Role Tenant 07", "admin", "Administrator")
        );

        assertThatThrownBy(() -> roleDirectory.updateRole(
            new UpdateRoleCommand("rolten07", "missing-role", "Missing Role")
        ))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessage("Role not found for tenant/code: ROLTEN07/MISSING-ROLE");
    }
}
