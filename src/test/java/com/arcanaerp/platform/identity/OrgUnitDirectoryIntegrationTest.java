package com.arcanaerp.platform.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.arcanaerp.platform.core.pagination.PageQuery;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class OrgUnitDirectoryIntegrationTest {

    @Autowired
    private OrgUnitDirectory orgUnitDirectory;

    @Test
    void registersAndListsOrgUnitsByTenant() {
        orgUnitDirectory.registerOrgUnit(
            new RegisterOrgUnitCommand("tenant01", "Tenant 01", "ops", "Operations")
        );
        orgUnitDirectory.registerOrgUnit(
            new RegisterOrgUnitCommand("tenant01", "Tenant 01", "hr", "Human Resources")
        );

        var orgUnits = orgUnitDirectory.listOrgUnits("tenant01", new PageQuery(0, 10), null);

        assertThat(orgUnits.page()).isEqualTo(0);
        assertThat(orgUnits.size()).isEqualTo(10);
        assertThat(orgUnits.totalItems()).isEqualTo(2);
        assertThat(orgUnits.items()).hasSize(2);
        assertThat(orgUnits.items()).extracting(OrgUnitView::code).containsExactlyInAnyOrder("OPS", "HR");
        assertThat(orgUnits.items()).extracting(OrgUnitView::tenantCode).containsOnly("TENANT01");
    }

    @Test
    void rejectsDuplicateOrgUnitCodeInTenant() {
        orgUnitDirectory.registerOrgUnit(
            new RegisterOrgUnitCommand("tenant02", "Tenant 02", "ops", "Operations")
        );

        assertThatThrownBy(() ->
            orgUnitDirectory.registerOrgUnit(
                new RegisterOrgUnitCommand("tenant02", "Tenant 02", "OPS", "Operations Copy")
            )
        )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Org unit code already exists in tenant: OPS");
    }
}
