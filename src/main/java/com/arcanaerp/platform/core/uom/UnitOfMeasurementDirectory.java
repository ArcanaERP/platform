package com.arcanaerp.platform.core.uom;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;

public interface UnitOfMeasurementDirectory {

    UnitOfMeasurementView registerUnitOfMeasurement(RegisterUnitOfMeasurementCommand command);

    PageResult<UnitOfMeasurementView> listUnitsOfMeasurement(PageQuery pageQuery, String queryFilter, String domain);
}
