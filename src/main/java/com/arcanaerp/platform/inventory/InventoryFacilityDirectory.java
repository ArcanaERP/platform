package com.arcanaerp.platform.inventory;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import java.time.Instant;

public interface InventoryFacilityDirectory {

    InventoryFacilityView registerFacility(RegisterInventoryFacilityCommand command);

    InventoryFacilityView facilityByCode(String code);

    InventoryFacilityView updateFacilityActive(String code, UpdateInventoryFacilityActiveCommand command);

    InventoryFacilityView updateFacilityMetadata(String code, UpdateInventoryFacilityMetadataCommand command);

    PageResult<InventoryFacilityActiveChangeView> listActiveHistory(
        String code,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    );

    PageResult<InventoryFacilityMetadataChangeView> listMetadataHistory(
        String code,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    );

    PageResult<InventoryFacilityView> listFacilities(Boolean active, String query, PageQuery pageQuery);
}
