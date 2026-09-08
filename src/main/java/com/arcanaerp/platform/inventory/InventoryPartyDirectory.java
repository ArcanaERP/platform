package com.arcanaerp.platform.inventory;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;

public interface InventoryPartyDirectory {

    InventoryPartyView registerParty(RegisterInventoryPartyCommand command);

    InventoryPartyView partyByCode(String code);

    boolean partyExists(String code);

    PageResult<InventoryPartyView> listParties(PageQuery pageQuery);
}
