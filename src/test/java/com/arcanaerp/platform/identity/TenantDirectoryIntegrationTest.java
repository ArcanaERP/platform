package com.arcanaerp.platform.identity;

import static org.assertj.core.api.Assertions.assertThat;

import com.arcanaerp.platform.core.pagination.PageQuery;
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
}
