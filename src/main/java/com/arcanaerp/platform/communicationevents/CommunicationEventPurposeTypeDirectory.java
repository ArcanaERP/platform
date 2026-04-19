package com.arcanaerp.platform.communicationevents;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;

public interface CommunicationEventPurposeTypeDirectory {

    CommunicationEventPurposeTypeView registerPurposeType(RegisterCommunicationEventPurposeTypeCommand command);

    CommunicationEventPurposeTypeView purposeTypeByCode(String tenantCode, String code);

    PageResult<CommunicationEventPurposeTypeView> listPurposeTypes(String tenantCode, PageQuery pageQuery);
}
