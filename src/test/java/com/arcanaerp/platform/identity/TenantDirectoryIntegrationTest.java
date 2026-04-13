package com.arcanaerp.platform.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.arcanaerp.platform.core.pagination.PageQuery;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class TenantDirectoryIntegrationTest {

    @Autowired
    private UserDirectory userDirectory;

    @Autowired
    private RoleDirectory roleDirectory;

    @Autowired
    private TenantDirectory tenantDirectory;

    @Test
    void listsTenantsCreatedThroughIdentityFlows() {
        roleDirectory.registerRole(
            new RegisterRoleCommand("tenantdir01", "Tenant Dir 01", "admin", "Administrator")
        );
        userDirectory.registerUser(
            new RegisterUserCommand(
                "tenantdir02",
                "Tenant Dir 02",
                "operator",
                "Operator",
                "tenantdir02@acme.com",
                "Tenant Dir User"
            )
        );

        var tenants = tenantDirectory.listTenants(new PageQuery(0, 20));

        assertThat(tenants.items()).extracting(TenantView::code).contains("TENANTDIR01", "TENANTDIR02");
        assertThat(tenants.items()).extracting(TenantView::name).contains("Tenant Dir 01", "Tenant Dir 02");
    }

    @Test
    void readsTenantByCode() {
        roleDirectory.registerRole(
            new RegisterRoleCommand("tenantdir03", "Tenant Dir 03", "admin", "Administrator")
        );

        TenantView tenant = tenantDirectory.tenantByCode("tenantdir03");

        assertThat(tenant.code()).isEqualTo("TENANTDIR03");
        assertThat(tenant.name()).isEqualTo("Tenant Dir 03");
    }

    @Test
    void rejectsMissingTenantByCode() {
        assertThatThrownBy(() -> tenantDirectory.tenantByCode("missing-tenant-dir"))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessage("Tenant not found: MISSING-TENANT-DIR");
    }
}
