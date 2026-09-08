package com.arcanaerp.platform.inventory;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;

public interface InventoryPartyRoleTypeDirectory {

    InventoryPartyRoleTypeView registerRoleType(RegisterInventoryPartyRoleTypeCommand command);

    InventoryPartyRoleTypeView roleTypeByCode(String code);

    boolean roleTypeExists(String code);

    PageResult<InventoryPartyRoleTypeView> listRoleTypes(PageQuery pageQuery);
}
