package com.arcanaerp.platform.inventory;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import java.time.Instant;
import java.util.UUID;

public interface InventoryPostalAddressDirectory {

    InventoryPostalAddressView registerPostalAddress(RegisterInventoryPostalAddressCommand command);

    InventoryPostalAddressView postalAddressById(UUID id);

    InventoryPostalAddressView updatePostalAddressMetadata(UUID id, UpdateInventoryPostalAddressMetadataCommand command);

    PageResult<InventoryPostalAddressMetadataChangeView> listMetadataHistory(
        UUID id,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    );

    PageResult<InventoryPostalAddressView> listPostalAddresses(
        String ownerType,
        String ownerCode,
        String addressPurposeCode,
        PageQuery pageQuery
    );
}
