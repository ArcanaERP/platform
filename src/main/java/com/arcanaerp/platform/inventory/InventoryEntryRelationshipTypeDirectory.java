package com.arcanaerp.platform.inventory;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;

public interface InventoryEntryRelationshipTypeDirectory {

    InventoryEntryRelationshipTypeView registerRelationshipType(RegisterInventoryEntryRelationshipTypeCommand command);

    InventoryEntryRelationshipTypeView relationshipTypeByCode(String code);

    boolean relationshipTypeExists(String code);

    PageResult<InventoryEntryRelationshipTypeView> listRelationshipTypes(PageQuery pageQuery);
}
