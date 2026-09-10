package com.arcanaerp.platform.inventory;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import java.util.UUID;

public interface InventoryPartyFixedAssetAssignmentDirectory {

    InventoryPartyFixedAssetAssignmentView registerAssignment(RegisterInventoryPartyFixedAssetAssignmentCommand command);

    InventoryPartyFixedAssetAssignmentView assignmentById(UUID id);

    PageResult<InventoryPartyFixedAssetAssignmentView> listAssignments(
        String partyCode,
        String fixedAssetCode,
        Long allocatedCostMoneyId,
        PageQuery pageQuery
    );
}
