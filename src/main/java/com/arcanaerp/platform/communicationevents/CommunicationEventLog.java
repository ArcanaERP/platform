package com.arcanaerp.platform.communicationevents;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;

public interface CommunicationEventLog {

    CommunicationEventView createEvent(CreateCommunicationEventCommand command);

    CommunicationEventView getEvent(String tenantCode, String eventNumber);

    PageResult<CommunicationEventView> listEvents(
        String tenantCode,
        PageQuery pageQuery,
        String statusCode,
        String purposeCode,
        String channel,
        String direction,
        String recordedBy
    );
}
