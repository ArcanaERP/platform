package com.arcanaerp.platform.communicationevents;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;

public interface CommunicationEventStatusTypeDirectory {

    CommunicationEventStatusTypeView registerStatusType(RegisterCommunicationEventStatusTypeCommand command);

    CommunicationEventStatusTypeView statusTypeByCode(String tenantCode, String code);

    PageResult<CommunicationEventStatusTypeView> listStatusTypes(String tenantCode, PageQuery pageQuery);
}
