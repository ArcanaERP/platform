package com.arcanaerp.platform.inventory;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;

public interface InventoryFixedAssetFacilityAssignmentTypeDirectory {

    InventoryFixedAssetFacilityAssignmentTypeView registerAssignmentType(
        RegisterInventoryFixedAssetFacilityAssignmentTypeCommand command
    );

    InventoryFixedAssetFacilityAssignmentTypeView assignmentTypeByCode(String code);

    boolean assignmentTypeExists(String code);

    PageResult<InventoryFixedAssetFacilityAssignmentTypeView> listAssignmentTypes(PageQuery pageQuery);
}
