package com.arcanaerp.platform.inventory;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;

public interface InventoryFixedAssetFacilityAssignmentTypeDirectory {

    InventoryFixedAssetFacilityAssignmentTypeView registerAssignmentType(
        RegisterInventoryFixedAssetFacilityAssignmentTypeCommand command
    );

    InventoryFixedAssetFacilityAssignmentTypeView assignmentTypeByCode(String code);

    InventoryFixedAssetFacilityAssignmentTypeView updateAssignmentTypeMetadata(
        String code,
        UpdateInventoryFixedAssetFacilityAssignmentTypeMetadataCommand command
    );

    boolean assignmentTypeExists(String code);

    PageResult<InventoryFixedAssetFacilityAssignmentTypeMetadataChangeView> listMetadataHistory(
        String code,
        String changedBy,
        java.time.Instant changedAtFrom,
        java.time.Instant changedAtTo,
        PageQuery pageQuery
    );

    PageResult<InventoryFixedAssetFacilityAssignmentTypeView> listAssignmentTypes(PageQuery pageQuery);
}
