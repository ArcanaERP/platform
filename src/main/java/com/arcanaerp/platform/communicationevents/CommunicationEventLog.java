package com.arcanaerp.platform.communicationevents;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import java.time.Instant;

public interface CommunicationEventLog {

    CommunicationEventView createEvent(CreateCommunicationEventCommand command);

    CommunicationEventView changeStatus(ChangeCommunicationEventStatusCommand command);

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

    PageResult<CommunicationEventStatusChangeView> listStatusHistory(
        String tenantCode,
        String eventNumber,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    );
}
