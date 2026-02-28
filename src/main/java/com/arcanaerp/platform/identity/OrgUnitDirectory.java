package com.arcanaerp.platform.identity;

import java.util.List;

public interface OrgUnitDirectory {

    OrgUnitView registerOrgUnit(RegisterOrgUnitCommand command);

    List<OrgUnitView> listOrgUnits(String tenantCode);
}
