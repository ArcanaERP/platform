package com.arcanaerp.platform.inventory;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;

public interface InventoryEntryRoleTypeDirectory {

    InventoryEntryRoleTypeView registerRoleType(RegisterInventoryEntryRoleTypeCommand command);

    InventoryEntryRoleTypeView roleTypeByCode(String code);

    boolean roleTypeExists(String code);

    PageResult<InventoryEntryRoleTypeView> listRoleTypes(PageQuery pageQuery);
}
