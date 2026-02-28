package com.arcanaerp.platform.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
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

        List<OrgUnitView> orgUnits = orgUnitDirectory.listOrgUnits("tenant01");

        assertThat(orgUnits).hasSize(2);
        assertThat(orgUnits).extracting(OrgUnitView::code).containsExactlyInAnyOrder("OPS", "HR");
        assertThat(orgUnits).extracting(OrgUnitView::tenantCode).containsOnly("TENANT01");
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
