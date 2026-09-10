package com.arcanaerp.platform.workeffort.web;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.arcanaerp.platform.workeffort.WorkEffortDeterministicClockTestSupport;
import com.arcanaerp.platform.testsupport.web.ActorActivationWebTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(WorkEffortDeterministicClockTestSupport.Configuration.class)
class WorkEffortsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WorkEffortDeterministicClockTestSupport.AdjustableClock testClock;

    @BeforeEach
    void resetClock() {
        testClock.resetToBaseInstant();
    }

    @Test
    void createsReadsAndListsWorkEfforts() throws Exception {
        ActorActivationWebTestSupport.registerActorAllowingDuplicateEmail(
            mockMvc,
            "workweb01",
            "agent01@work.com",
            "Work Web",
            "Agent 01"
        );
        ActorActivationWebTestSupport.registerActorAllowingDuplicateEmail(
            mockMvc,
            "workweb01",
            "agent02@work.com",
            "Work Web",
            "Agent 02"
        );

        WorkEffortsWebIntegrationTestSupport.createWorkEffort(
            mockMvc,
            "workweb01",
            "we-001",
            "Prepare shipment",
            "Prepare shipment for dispatch",
            "PLANNED",
            "AGENT01@WORK.COM",
            "2026-04-22T10:00:00Z"
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tenantCode").value("WORKWEB01"))
            .andExpect(jsonPath("$.effortNumber").value("WE-001"))
            .andExpect(jsonPath("$.status").value("PLANNED"))
            .andExpect(jsonPath("$.assignedTo").value("agent01@work.com"));

        WorkEffortsWebIntegrationTestSupport.createWorkEffort(
            mockMvc,
            "workweb01",
            "we-002",
            "Confirm receipt",
            "Confirm inbound receipt",
            "IN_PROGRESS",
            "agent02@work.com",
            null
        )
            .andExpect(status().isCreated());

        mockMvc.perform(WorkEffortsWebIntegrationTestSupport.getWorkEffortRequest("workweb01", "we-001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.effortNumber").value("WE-001"))
            .andExpect(jsonPath("$.name").value("Prepare shipment"));

        mockMvc.perform(
            WorkEffortsWebIntegrationTestSupport.listWorkEffortsRequest(
                "workweb01",
                0,
                10,
                "status", "PLANNED",
                "assignedTo", "agent01@work.com"
            )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(10))
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[?(@.effortNumber=='WE-001')].name", hasItem("Prepare shipment")));
    }

    @Test
    void usesDefaultPaginationWhenPageAndSizeOmitted() throws Exception {
        ActorActivationWebTestSupport.registerActorAllowingDuplicateEmail(
            mockMvc,
            "workweb02",
            "agent01@work.com",
            "Work Web",
            "Agent 01"
        );

        WorkEffortsWebIntegrationTestSupport.createWorkEffort(
            mockMvc,
            "workweb02",
            "we-001",
            "Prepare shipment",
            "Prepare shipment for dispatch",
            "PLANNED",
            "agent01@work.com",
            null
        )
            .andExpect(status().isCreated());

        mockMvc.perform(WorkEffortsWebIntegrationTestSupport.listWorkEffortsRequest("workweb02"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.totalItems", greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.items[?(@.effortNumber=='WE-001')].name", hasItem("Prepare shipment")));
    }

    @Test
    void rejectsDuplicateTenantLocalEffortNumbers() throws Exception {
        ActorActivationWebTestSupport.registerActorAllowingDuplicateEmail(
            mockMvc,
            "workweb03",
            "agent01@work.com",
            "Work Web",
            "Agent 01"
        );

        WorkEffortsWebIntegrationTestSupport.createWorkEffort(
            mockMvc,
            "workweb03",
            "we-001",
            "Prepare shipment",
            "Prepare shipment for dispatch",
            "PLANNED",
            "agent01@work.com",
            null
        )
            .andExpect(status().isCreated());

        WorkEffortsWebIntegrationTestSupport.createWorkEffort(
            mockMvc,
            "workweb03",
            "WE-001",
            "Duplicate effort",
            "Duplicate effort description",
            "PLANNED",
            "agent01@work.com",
            null
        )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.error").value("Conflict"))
            .andExpect(jsonPath("$.message").value("Work effort already exists for tenant/effortNumber: WORKWEB03/WE-001"))
            .andExpect(jsonPath("$.path").value("/api/work-efforts"));
    }

    @Test
    void createsReadsAndListsAssociatedWorkEfforts() throws Exception {
        ActorActivationWebTestSupport.registerActorAllowingDuplicateEmail(
            mockMvc,
            "workwebassocrec01",
            "agent01@work.com",
            "Work Web",
            "Agent 01"
        );
        WorkEffortsWebIntegrationTestSupport.createWorkEffort(
            mockMvc,
            "workwebassocrec01",
            "we-record-001",
            "Stage outbound shipment",
            "Stage outbound shipment for pickup",
            "PLANNED",
            "agent01@work.com",
            null
        )
            .andExpect(status().isCreated());

        String associatedWorkEffortId = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
            "/api/work-efforts/associated-records"
        )
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {
                  "tenantCode": " workwebassocrec01 ",
                  "effortNumber": " we-record-001 ",
                  "associatedRecordId": 44,
                  "associatedRecordType": "ShipmentItem"
                }
                """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.workEffortId").isNotEmpty())
            .andExpect(jsonPath("$.tenantCode").value("WORKWEBASSOCREC01"))
            .andExpect(jsonPath("$.effortNumber").value("WE-RECORD-001"))
            .andExpect(jsonPath("$.associatedRecordId").value(44))
            .andExpect(jsonPath("$.associatedRecordType").value("ShipmentItem"))
            .andReturn()
            .getResponse()
            .getContentAsString()
            .replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
            "/api/work-efforts/associated-records/{id}",
            associatedWorkEffortId
        ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(associatedWorkEffortId))
            .andExpect(jsonPath("$.associatedRecordType").value("ShipmentItem"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
            "/api/work-efforts/associated-records"
        )
            .param("tenantCode", "workwebassocrec01")
            .param("effortNumber", "we-record-001")
            .param("associatedRecordId", "44")
            .param("associatedRecordType", "ShipmentItem")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].id").value(associatedWorkEffortId))
            .andExpect(jsonPath("$.items[0].tenantCode").value("WORKWEBASSOCREC01"))
            .andExpect(jsonPath("$.items[0].effortNumber").value("WE-RECORD-001"))
            .andExpect(jsonPath("$.items[0].associatedRecordId").value(44))
            .andExpect(jsonPath("$.items[0].associatedRecordType").value("ShipmentItem"));
    }

    @Test
    void allowsDuplicateAssociatedWorkEffortsForLegacyParity() throws Exception {
        ActorActivationWebTestSupport.registerActorAllowingDuplicateEmail(
            mockMvc,
            "workwebassocrec02",
            "agent01@work.com",
            "Work Web",
            "Agent 01"
        );
        WorkEffortsWebIntegrationTestSupport.createWorkEffort(
            mockMvc,
            "workwebassocrec02",
            "we-record-001",
            "Pick shipment",
            "Pick shipment items",
            "PLANNED",
            "agent01@work.com",
            null
        )
            .andExpect(status().isCreated());
        String payload = """
            {
              "tenantCode": "workwebassocrec02",
              "effortNumber": "we-record-001",
              "associatedRecordId": 44,
              "associatedRecordType": "ShipmentItem"
            }
            """;

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
            "/api/work-efforts/associated-records"
        )
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content(payload))
            .andExpect(status().isCreated());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
            "/api/work-efforts/associated-records"
        )
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content(payload))
            .andExpect(status().isCreated());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
            "/api/work-efforts/associated-records"
        )
            .param("tenantCode", "workwebassocrec02")
            .param("effortNumber", "we-record-001")
            .param("associatedRecordId", "44")
            .param("associatedRecordType", "ShipmentItem")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(2));
    }

    @Test
    void rejectsAssociatedWorkEffortForMissingWorkEffort() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
            "/api/work-efforts/associated-records"
        )
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {
                  "tenantCode": "workwebassocrec03",
                  "effortNumber": "missing",
                  "associatedRecordId": 44,
                  "associatedRecordType": "ShipmentItem"
                }
                """))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value(
                "Work effort not found for tenant/effortNumber: WORKWEBASSOCREC03/MISSING"
            ))
            .andExpect(jsonPath("$.path").value("/api/work-efforts/associated-records"));
    }

    @Test
    void createsReadsAndListsWorkOrderItemFulfillments() throws Exception {
        ActorActivationWebTestSupport.registerActorAllowingDuplicateEmail(
            mockMvc,
            "workwebfulfill01",
            "agent01@work.com",
            "Work Web",
            "Agent 01"
        );
        WorkEffortsWebIntegrationTestSupport.createWorkEffort(
            mockMvc,
            "workwebfulfill01",
            "we-fulfill-001",
            "Build customer kit",
            "Build kit for sales order line",
            "PLANNED",
            "agent01@work.com",
            null
        )
            .andExpect(status().isCreated());

        String fulfillmentId = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
            "/api/work-efforts/work-order-item-fulfillments"
        )
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {
                  "tenantCode": " workwebfulfill01 ",
                  "effortNumber": " we-fulfill-001 ",
                  "orderLineItemId": 77,
                  "description": " Kit build "
                }
                """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.workEffortId").isNotEmpty())
            .andExpect(jsonPath("$.tenantCode").value("WORKWEBFULFILL01"))
            .andExpect(jsonPath("$.effortNumber").value("WE-FULFILL-001"))
            .andExpect(jsonPath("$.orderLineItemId").value(77))
            .andExpect(jsonPath("$.description").value("Kit build"))
            .andExpect(jsonPath("$.createdAt").isNotEmpty())
            .andReturn()
            .getResponse()
            .getContentAsString()
            .replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
            "/api/work-efforts/work-order-item-fulfillments/{id}",
            fulfillmentId
        ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(fulfillmentId))
            .andExpect(jsonPath("$.orderLineItemId").value(77));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
            "/api/work-efforts/work-order-item-fulfillments"
        )
            .param("tenantCode", "workwebfulfill01")
            .param("effortNumber", "we-fulfill-001")
            .param("orderLineItemId", "77")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].id").value(fulfillmentId))
            .andExpect(jsonPath("$.items[0].tenantCode").value("WORKWEBFULFILL01"))
            .andExpect(jsonPath("$.items[0].effortNumber").value("WE-FULFILL-001"))
            .andExpect(jsonPath("$.items[0].orderLineItemId").value(77));
    }

    @Test
    void allowsDuplicateWorkOrderItemFulfillmentsForLegacyParity() throws Exception {
        ActorActivationWebTestSupport.registerActorAllowingDuplicateEmail(
            mockMvc,
            "workwebfulfill02",
            "agent01@work.com",
            "Work Web",
            "Agent 01"
        );
        WorkEffortsWebIntegrationTestSupport.createWorkEffort(
            mockMvc,
            "workwebfulfill02",
            "we-fulfill-001",
            "Reserve custom item",
            "Reserve custom item for order line",
            "PLANNED",
            "agent01@work.com",
            null
        )
            .andExpect(status().isCreated());
        String payload = """
            {
              "tenantCode": "workwebfulfill02",
              "effortNumber": "we-fulfill-001",
              "orderLineItemId": 77,
              "description": "Reserve item"
            }
            """;

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
            "/api/work-efforts/work-order-item-fulfillments"
        )
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content(payload))
            .andExpect(status().isCreated());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
            "/api/work-efforts/work-order-item-fulfillments"
        )
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content(payload))
            .andExpect(status().isCreated());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
            "/api/work-efforts/work-order-item-fulfillments"
        )
            .param("tenantCode", "workwebfulfill02")
            .param("effortNumber", "we-fulfill-001")
            .param("orderLineItemId", "77")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(2));
    }

    @Test
    void rejectsWorkOrderItemFulfillmentForMissingWorkEffort() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
            "/api/work-efforts/work-order-item-fulfillments"
        )
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {
                  "tenantCode": "workwebfulfill03",
                  "effortNumber": "missing",
                  "orderLineItemId": 77,
                  "description": "Reserve item"
                }
                """))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value(
                "Work effort not found for tenant/effortNumber: WORKWEBFULFILL03/MISSING"
            ))
            .andExpect(jsonPath("$.path").value("/api/work-efforts/work-order-item-fulfillments"));
    }

    @Test
    void createsReadsAndListsWorkEffortAssociationTypes() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
            "/api/work-efforts/association-types"
        )
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {
                  "code": " dependency ",
                  "name": " Dependency ",
                  "description": "One work effort depends on another",
                  "parentTypeCode": " schedule ",
                  "validFromRoleTypeCode": " predecessor ",
                  "validToRoleTypeCode": " successor ",
                  "externalIdentifier": "legacy-dependency",
                  "externalIdSource": "erp_work_effort"
                }
                """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.code").value("DEPENDENCY"))
            .andExpect(jsonPath("$.name").value("Dependency"))
            .andExpect(jsonPath("$.parentTypeCode").value("SCHEDULE"))
            .andExpect(jsonPath("$.validFromRoleTypeCode").value("PREDECESSOR"))
            .andExpect(jsonPath("$.validToRoleTypeCode").value("SUCCESSOR"))
            .andExpect(jsonPath("$.externalIdentifier").value("legacy-dependency"))
            .andExpect(jsonPath("$.externalIdSource").value("erp_work_effort"))
            .andExpect(jsonPath("$.createdAt").isNotEmpty());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
            "/api/work-efforts/association-types/{code}",
            "dependency"
        ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("DEPENDENCY"))
            .andExpect(jsonPath("$.name").value("Dependency"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
            "/api/work-efforts/association-types"
        )
            .param("parentTypeCode", "schedule")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].code").value("DEPENDENCY"));
    }

    @Test
    void rejectsDuplicateWorkEffortAssociationTypes() throws Exception {
        String payload = """
            {
              "code": "breakdown",
              "name": "Breakdown"
            }
            """;

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
            "/api/work-efforts/association-types"
        )
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content(payload))
            .andExpect(status().isCreated());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
            "/api/work-efforts/association-types"
        )
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content(payload))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.message").value("Work effort association type already exists: BREAKDOWN"));
    }

    @Test
    void createsReadsAndListsWorkEffortAssociations() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
            "/api/work-efforts/association-types"
        )
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {
                  "code": "precedence",
                  "name": "Precedence"
                }
                """))
            .andExpect(status().isCreated());

        ActorActivationWebTestSupport.registerActorAllowingDuplicateEmail(
            mockMvc,
            "workwebassoc01",
            "agent01@work.com",
            "Work Web",
            "Agent 01"
        );
        WorkEffortsWebIntegrationTestSupport.createWorkEffort(
            mockMvc,
            "workwebassoc01",
            "we-assoc-from",
            "Pick order",
            "Pick order lines",
            "PLANNED",
            "agent01@work.com",
            null
        )
            .andExpect(status().isCreated());
        WorkEffortsWebIntegrationTestSupport.createWorkEffort(
            mockMvc,
            "workwebassoc01",
            "we-assoc-to",
            "Pack order",
            "Pack order lines",
            "PLANNED",
            "agent01@work.com",
            null
        )
            .andExpect(status().isCreated());

        String associationId = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
            "/api/work-efforts/associations"
        )
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {
                  "tenantCode": " workwebassoc01 ",
                  "associationTypeCode": " precedence ",
                  "description": " Pick before pack ",
                  "fromEffortNumber": " we-assoc-from ",
                  "toEffortNumber": " we-assoc-to ",
                  "fromRoleTypeCode": " predecessor ",
                  "toRoleTypeCode": " successor ",
                  "relationshipTypeCode": " depends-on ",
                  "effectiveFrom": "2026-04-22T10:00:00Z",
                  "effectiveThru": "2026-04-23T10:00:00Z"
                }
                """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.tenantCode").value("WORKWEBASSOC01"))
            .andExpect(jsonPath("$.associationTypeCode").value("PRECEDENCE"))
            .andExpect(jsonPath("$.description").value("Pick before pack"))
            .andExpect(jsonPath("$.fromWorkEffortId").isNotEmpty())
            .andExpect(jsonPath("$.fromEffortNumber").value("WE-ASSOC-FROM"))
            .andExpect(jsonPath("$.toWorkEffortId").isNotEmpty())
            .andExpect(jsonPath("$.toEffortNumber").value("WE-ASSOC-TO"))
            .andExpect(jsonPath("$.fromRoleTypeCode").value("PREDECESSOR"))
            .andExpect(jsonPath("$.toRoleTypeCode").value("SUCCESSOR"))
            .andExpect(jsonPath("$.relationshipTypeCode").value("DEPENDS-ON"))
            .andExpect(jsonPath("$.effectiveFrom").value("2026-04-22T10:00:00Z"))
            .andExpect(jsonPath("$.effectiveThru").value("2026-04-23T10:00:00Z"))
            .andExpect(jsonPath("$.createdAt").isNotEmpty())
            .andReturn()
            .getResponse()
            .getContentAsString()
            .replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
            "/api/work-efforts/associations/{id}",
            associationId
        ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(associationId))
            .andExpect(jsonPath("$.associationTypeCode").value("PRECEDENCE"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
            "/api/work-efforts/associations"
        )
            .param("tenantCode", "workwebassoc01")
            .param("associationTypeCode", "precedence")
            .param("fromEffortNumber", "we-assoc-from")
            .param("toEffortNumber", "we-assoc-to")
            .param("relationshipTypeCode", "depends-on")
            .param("effectiveFrom", "2026-04-22T09:00:00Z")
            .param("effectiveThru", "2026-04-23T11:00:00Z")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].id").value(associationId))
            .andExpect(jsonPath("$.items[0].fromEffortNumber").value("WE-ASSOC-FROM"))
            .andExpect(jsonPath("$.items[0].toEffortNumber").value("WE-ASSOC-TO"));
    }

    @Test
    void allowsDuplicateWorkEffortAssociationsForLegacyParity() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
            "/api/work-efforts/association-types"
        )
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {
                  "code": "concurrence",
                  "name": "Concurrence"
                }
                """))
            .andExpect(status().isCreated());

        ActorActivationWebTestSupport.registerActorAllowingDuplicateEmail(
            mockMvc,
            "workwebassoc02",
            "agent01@work.com",
            "Work Web",
            "Agent 01"
        );
        WorkEffortsWebIntegrationTestSupport.createWorkEffort(
            mockMvc,
            "workwebassoc02",
            "we-assoc-from",
            "Load truck",
            "Load truck",
            "PLANNED",
            "agent01@work.com",
            null
        )
            .andExpect(status().isCreated());
        WorkEffortsWebIntegrationTestSupport.createWorkEffort(
            mockMvc,
            "workwebassoc02",
            "we-assoc-to",
            "Seal truck",
            "Seal truck",
            "PLANNED",
            "agent01@work.com",
            null
        )
            .andExpect(status().isCreated());
        String payload = """
            {
              "tenantCode": "workwebassoc02",
              "associationTypeCode": "concurrence",
              "fromEffortNumber": "we-assoc-from",
              "toEffortNumber": "we-assoc-to"
            }
            """;

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
            "/api/work-efforts/associations"
        )
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content(payload))
            .andExpect(status().isCreated());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
            "/api/work-efforts/associations"
        )
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content(payload))
            .andExpect(status().isCreated());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
            "/api/work-efforts/associations"
        )
            .param("tenantCode", "workwebassoc02")
            .param("associationTypeCode", "concurrence")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(2));
    }

    @Test
    void rejectsWorkEffortAssociationForMissingAssociationType() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
            "/api/work-efforts/associations"
        )
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {
                  "tenantCode": "workwebassoc03",
                  "associationTypeCode": "missing",
                  "fromEffortNumber": "we-assoc-from",
                  "toEffortNumber": "we-assoc-to"
                }
                """))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("Work effort association type not found: MISSING"))
            .andExpect(jsonPath("$.path").value("/api/work-efforts/associations"));
    }

    @Test
    void rejectsWorkEffortAssociationWhenEffectiveFromIsAfterEffectiveThru() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
            "/api/work-efforts/association-types"
        )
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {
                  "code": "invalid-window",
                  "name": "Invalid Window"
                }
                """))
            .andExpect(status().isCreated());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
            "/api/work-efforts/associations"
        )
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {
                  "tenantCode": "workwebassoc04",
                  "associationTypeCode": "invalid-window",
                  "fromEffortNumber": "from",
                  "toEffortNumber": "to",
                  "effectiveFrom": "2026-04-23T10:00:00Z",
                  "effectiveThru": "2026-04-22T10:00:00Z"
                }
                """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("effectiveFrom must be before or equal to effectiveThru"))
            .andExpect(jsonPath("$.path").value("/api/work-efforts/associations"));
    }

    @Test
    void createsReadsAndListsWorkEffortFixedAssetAssignments() throws Exception {
        ActorActivationWebTestSupport.registerActorAllowingDuplicateEmail(
            mockMvc,
            "workwebasset01",
            "agent01@work.com",
            "Work Web",
            "Agent 01"
        );
        WorkEffortsWebIntegrationTestSupport.createWorkEffort(
            mockMvc,
            "workwebasset01",
            "we-asset-001",
            "Repair forklift",
            "Repair forklift hydraulic line",
            "PLANNED",
            "agent01@work.com",
            null
        )
            .andExpect(status().isCreated());

        String assignmentId = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
            "/api/work-efforts/fixed-asset-assignments"
        )
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {
                  "tenantCode": " workwebasset01 ",
                  "effortNumber": " we-asset-001 ",
                  "fixedAssetCode": " forklift-77 "
                }
                """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.workEffortId").isNotEmpty())
            .andExpect(jsonPath("$.tenantCode").value("WORKWEBASSET01"))
            .andExpect(jsonPath("$.effortNumber").value("WE-ASSET-001"))
            .andExpect(jsonPath("$.fixedAssetCode").value("FORKLIFT-77"))
            .andExpect(jsonPath("$.createdAt").isNotEmpty())
            .andReturn()
            .getResponse()
            .getContentAsString()
            .replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
            "/api/work-efforts/fixed-asset-assignments/{id}",
            assignmentId
        ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(assignmentId))
            .andExpect(jsonPath("$.fixedAssetCode").value("FORKLIFT-77"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
            "/api/work-efforts/fixed-asset-assignments"
        )
            .param("tenantCode", "workwebasset01")
            .param("effortNumber", "we-asset-001")
            .param("fixedAssetCode", "forklift-77")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].id").value(assignmentId))
            .andExpect(jsonPath("$.items[0].tenantCode").value("WORKWEBASSET01"))
            .andExpect(jsonPath("$.items[0].effortNumber").value("WE-ASSET-001"))
            .andExpect(jsonPath("$.items[0].fixedAssetCode").value("FORKLIFT-77"));
    }

    @Test
    void allowsDuplicateWorkEffortFixedAssetAssignmentsForLegacyParity() throws Exception {
        ActorActivationWebTestSupport.registerActorAllowingDuplicateEmail(
            mockMvc,
            "workwebasset02",
            "agent01@work.com",
            "Work Web",
            "Agent 01"
        );
        WorkEffortsWebIntegrationTestSupport.createWorkEffort(
            mockMvc,
            "workwebasset02",
            "we-asset-001",
            "Repair conveyor",
            "Repair conveyor belt",
            "PLANNED",
            "agent01@work.com",
            null
        )
            .andExpect(status().isCreated());
        String payload = """
            {
              "tenantCode": "workwebasset02",
              "effortNumber": "we-asset-001",
              "fixedAssetCode": "conveyor-12"
            }
            """;

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
            "/api/work-efforts/fixed-asset-assignments"
        )
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content(payload))
            .andExpect(status().isCreated());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
            "/api/work-efforts/fixed-asset-assignments"
        )
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content(payload))
            .andExpect(status().isCreated());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
            "/api/work-efforts/fixed-asset-assignments"
        )
            .param("tenantCode", "workwebasset02")
            .param("effortNumber", "we-asset-001")
            .param("fixedAssetCode", "conveyor-12")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(2));
    }

    @Test
    void rejectsWorkEffortFixedAssetAssignmentForMissingWorkEffort() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
            "/api/work-efforts/fixed-asset-assignments"
        )
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {
                  "tenantCode": "workwebasset03",
                  "effortNumber": "missing",
                  "fixedAssetCode": "forklift-77"
                }
                """))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value(
                "Work effort not found for tenant/effortNumber: WORKWEBASSET03/MISSING"
            ))
            .andExpect(jsonPath("$.path").value("/api/work-efforts/fixed-asset-assignments"));
    }

    @Test
    void createsReadsAndListsWorkEffortInventoryAssignments() throws Exception {
        ActorActivationWebTestSupport.registerActorAllowingDuplicateEmail(
            mockMvc,
            "workwebinv01",
            "agent01@work.com",
            "Work Web",
            "Agent 01"
        );
        WorkEffortsWebIntegrationTestSupport.createWorkEffort(
            mockMvc,
            "workwebinv01",
            "we-inv-001",
            "Assemble repair kit",
            "Assemble repair kit for field work",
            "PLANNED",
            "agent01@work.com",
            null
        )
            .andExpect(status().isCreated());

        String assignmentId = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
            "/api/work-efforts/inventory-assignments"
        )
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {
                  "tenantCode": " workwebinv01 ",
                  "effortNumber": " we-inv-001 ",
                  "inventoryEntryCode": " filter-44 "
                }
                """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.workEffortId").isNotEmpty())
            .andExpect(jsonPath("$.tenantCode").value("WORKWEBINV01"))
            .andExpect(jsonPath("$.effortNumber").value("WE-INV-001"))
            .andExpect(jsonPath("$.inventoryEntryCode").value("FILTER-44"))
            .andExpect(jsonPath("$.createdAt").isNotEmpty())
            .andReturn()
            .getResponse()
            .getContentAsString()
            .replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
            "/api/work-efforts/inventory-assignments/{id}",
            assignmentId
        ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(assignmentId))
            .andExpect(jsonPath("$.inventoryEntryCode").value("FILTER-44"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
            "/api/work-efforts/inventory-assignments"
        )
            .param("tenantCode", "workwebinv01")
            .param("effortNumber", "we-inv-001")
            .param("inventoryEntryCode", "filter-44")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].id").value(assignmentId))
            .andExpect(jsonPath("$.items[0].tenantCode").value("WORKWEBINV01"))
            .andExpect(jsonPath("$.items[0].effortNumber").value("WE-INV-001"))
            .andExpect(jsonPath("$.items[0].inventoryEntryCode").value("FILTER-44"));
    }

    @Test
    void allowsDuplicateWorkEffortInventoryAssignmentsForLegacyParity() throws Exception {
        ActorActivationWebTestSupport.registerActorAllowingDuplicateEmail(
            mockMvc,
            "workwebinv02",
            "agent01@work.com",
            "Work Web",
            "Agent 01"
        );
        WorkEffortsWebIntegrationTestSupport.createWorkEffort(
            mockMvc,
            "workwebinv02",
            "we-inv-001",
            "Consume repair kit",
            "Consume repair kit on work effort",
            "PLANNED",
            "agent01@work.com",
            null
        )
            .andExpect(status().isCreated());
        String payload = """
            {
              "tenantCode": "workwebinv02",
              "effortNumber": "we-inv-001",
              "inventoryEntryCode": "filter-44"
            }
            """;

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
            "/api/work-efforts/inventory-assignments"
        )
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content(payload))
            .andExpect(status().isCreated());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
            "/api/work-efforts/inventory-assignments"
        )
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content(payload))
            .andExpect(status().isCreated());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
            "/api/work-efforts/inventory-assignments"
        )
            .param("tenantCode", "workwebinv02")
            .param("effortNumber", "we-inv-001")
            .param("inventoryEntryCode", "filter-44")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(2));
    }

    @Test
    void rejectsWorkEffortInventoryAssignmentForMissingWorkEffort() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
            "/api/work-efforts/inventory-assignments"
        )
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {
                  "tenantCode": "workwebinv03",
                  "effortNumber": "missing",
                  "inventoryEntryCode": "filter-44"
                }
                """))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value(
                "Work effort not found for tenant/effortNumber: WORKWEBINV03/MISSING"
            ))
            .andExpect(jsonPath("$.path").value("/api/work-efforts/inventory-assignments"));
    }

    @Test
    void createsReadsAndListsWorkEffortPartyAssignments() throws Exception {
        ActorActivationWebTestSupport.registerActorAllowingDuplicateEmail(
            mockMvc,
            "workwebparty01",
            "agent01@work.com",
            "Work Web",
            "Agent 01"
        );
        WorkEffortsWebIntegrationTestSupport.createWorkEffort(
            mockMvc,
            "workwebparty01",
            "we-party-001",
            "Inspect loading dock",
            "Inspect loading dock doors",
            "PLANNED",
            "agent01@work.com",
            null
        )
            .andExpect(status().isCreated());

        String assignmentId = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
            "/api/work-efforts/party-assignments"
        )
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {
                  "tenantCode": " workwebparty01 ",
                  "effortNumber": " we-party-001 ",
                  "partyCode": " vendor-17 ",
                  "roleTypeCode": " worker ",
                  "assignedFrom": "2026-04-22T10:00:00Z",
                  "assignedThru": "2026-04-22T12:00:00Z",
                  "comments": " Door inspection "
                }
                """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.workEffortId").isNotEmpty())
            .andExpect(jsonPath("$.tenantCode").value("WORKWEBPARTY01"))
            .andExpect(jsonPath("$.effortNumber").value("WE-PARTY-001"))
            .andExpect(jsonPath("$.partyCode").value("VENDOR-17"))
            .andExpect(jsonPath("$.roleTypeCode").value("WORKER"))
            .andExpect(jsonPath("$.assignedFrom").value("2026-04-22T10:00:00Z"))
            .andExpect(jsonPath("$.assignedThru").value("2026-04-22T12:00:00Z"))
            .andExpect(jsonPath("$.comments").value("Door inspection"))
            .andExpect(jsonPath("$.createdAt").isNotEmpty())
            .andReturn()
            .getResponse()
            .getContentAsString()
            .replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
            "/api/work-efforts/party-assignments/{id}",
            assignmentId
        ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(assignmentId))
            .andExpect(jsonPath("$.partyCode").value("VENDOR-17"))
            .andExpect(jsonPath("$.roleTypeCode").value("WORKER"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
            "/api/work-efforts/party-assignments"
        )
            .param("tenantCode", "workwebparty01")
            .param("effortNumber", "we-party-001")
            .param("partyCode", "vendor-17")
            .param("roleTypeCode", "worker")
            .param("assignedFrom", "2026-04-22T09:00:00Z")
            .param("assignedThru", "2026-04-22T13:00:00Z")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].id").value(assignmentId))
            .andExpect(jsonPath("$.items[0].tenantCode").value("WORKWEBPARTY01"))
            .andExpect(jsonPath("$.items[0].effortNumber").value("WE-PARTY-001"))
            .andExpect(jsonPath("$.items[0].partyCode").value("VENDOR-17"))
            .andExpect(jsonPath("$.items[0].roleTypeCode").value("WORKER"));
    }

    @Test
    void allowsDuplicateWorkEffortPartyAssignmentsForLegacyParity() throws Exception {
        ActorActivationWebTestSupport.registerActorAllowingDuplicateEmail(
            mockMvc,
            "workwebparty02",
            "agent01@work.com",
            "Work Web",
            "Agent 01"
        );
        WorkEffortsWebIntegrationTestSupport.createWorkEffort(
            mockMvc,
            "workwebparty02",
            "we-party-001",
            "Clean loading dock",
            "Clean loading dock floor",
            "PLANNED",
            "agent01@work.com",
            null
        )
            .andExpect(status().isCreated());
        String payload = """
            {
              "tenantCode": "workwebparty02",
              "effortNumber": "we-party-001",
              "partyCode": "vendor-17",
              "roleTypeCode": "worker",
              "assignedFrom": "2026-04-22T10:00:00Z",
              "assignedThru": "2026-04-22T12:00:00Z",
              "comments": "Clean dock"
            }
            """;

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
            "/api/work-efforts/party-assignments"
        )
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content(payload))
            .andExpect(status().isCreated());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
            "/api/work-efforts/party-assignments"
        )
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content(payload))
            .andExpect(status().isCreated());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
            "/api/work-efforts/party-assignments"
        )
            .param("tenantCode", "workwebparty02")
            .param("effortNumber", "we-party-001")
            .param("partyCode", "vendor-17")
            .param("roleTypeCode", "worker")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(2));
    }

    @Test
    void rejectsWorkEffortPartyAssignmentForMissingWorkEffort() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
            "/api/work-efforts/party-assignments"
        )
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {
                  "tenantCode": "workwebparty03",
                  "effortNumber": "missing",
                  "partyCode": "vendor-17",
                  "roleTypeCode": "worker"
                }
                """))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value(
                "Work effort not found for tenant/effortNumber: WORKWEBPARTY03/MISSING"
            ))
            .andExpect(jsonPath("$.path").value("/api/work-efforts/party-assignments"));
    }

    @Test
    void rejectsWorkEffortPartyAssignmentWhenAssignedFromIsAfterAssignedThru() throws Exception {
        ActorActivationWebTestSupport.registerActorAllowingDuplicateEmail(
            mockMvc,
            "workwebparty04",
            "agent01@work.com",
            "Work Web",
            "Agent 01"
        );
        WorkEffortsWebIntegrationTestSupport.createWorkEffort(
            mockMvc,
            "workwebparty04",
            "we-party-001",
            "Inspect compressor",
            "Inspect compressor room",
            "PLANNED",
            "agent01@work.com",
            null
        )
            .andExpect(status().isCreated());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
            "/api/work-efforts/party-assignments"
        )
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {
                  "tenantCode": "workwebparty04",
                  "effortNumber": "we-party-001",
                  "partyCode": "vendor-17",
                  "roleTypeCode": "worker",
                  "assignedFrom": "2026-04-22T13:00:00Z",
                  "assignedThru": "2026-04-22T12:00:00Z"
                }
                """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("assignedFrom must be before or equal to assignedThru"))
            .andExpect(jsonPath("$.path").value("/api/work-efforts/party-assignments"));
    }

    @Test
    void createsReadsAndListsWorkEffortRoleTypeAssignments() throws Exception {
        ActorActivationWebTestSupport.registerActorAllowingDuplicateEmail(
            mockMvc,
            "workwebrole01",
            "agent01@work.com",
            "Work Web",
            "Agent 01"
        );
        WorkEffortsWebIntegrationTestSupport.createWorkEffort(
            mockMvc,
            "workwebrole01",
            "we-role-001",
            "Coordinate receiving",
            "Coordinate receiving work",
            "PLANNED",
            "agent01@work.com",
            null
        )
            .andExpect(status().isCreated());

        String assignmentId = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
            "/api/work-efforts/role-type-assignments"
        )
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {
                  "tenantCode": " workwebrole01 ",
                  "effortNumber": " we-role-001 ",
                  "roleTypeCode": " task-master "
                }
                """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.workEffortId").isNotEmpty())
            .andExpect(jsonPath("$.tenantCode").value("WORKWEBROLE01"))
            .andExpect(jsonPath("$.effortNumber").value("WE-ROLE-001"))
            .andExpect(jsonPath("$.roleTypeCode").value("TASK-MASTER"))
            .andReturn()
            .getResponse()
            .getContentAsString()
            .replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
            "/api/work-efforts/role-type-assignments/{id}",
            assignmentId
        ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(assignmentId))
            .andExpect(jsonPath("$.roleTypeCode").value("TASK-MASTER"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
            "/api/work-efforts/role-type-assignments"
        )
            .param("tenantCode", "workwebrole01")
            .param("effortNumber", "we-role-001")
            .param("roleTypeCode", "task-master")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].id").value(assignmentId))
            .andExpect(jsonPath("$.items[0].tenantCode").value("WORKWEBROLE01"))
            .andExpect(jsonPath("$.items[0].effortNumber").value("WE-ROLE-001"))
            .andExpect(jsonPath("$.items[0].roleTypeCode").value("TASK-MASTER"));
    }

    @Test
    void allowsDuplicateWorkEffortRoleTypeAssignmentsForLegacyParity() throws Exception {
        ActorActivationWebTestSupport.registerActorAllowingDuplicateEmail(
            mockMvc,
            "workwebrole02",
            "agent01@work.com",
            "Work Web",
            "Agent 01"
        );
        WorkEffortsWebIntegrationTestSupport.createWorkEffort(
            mockMvc,
            "workwebrole02",
            "we-role-001",
            "Coordinate picking",
            "Coordinate picking work",
            "PLANNED",
            "agent01@work.com",
            null
        )
            .andExpect(status().isCreated());
        String payload = """
            {
              "tenantCode": "workwebrole02",
              "effortNumber": "we-role-001",
              "roleTypeCode": "task-master"
            }
            """;

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
            "/api/work-efforts/role-type-assignments"
        )
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content(payload))
            .andExpect(status().isCreated());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
            "/api/work-efforts/role-type-assignments"
        )
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content(payload))
            .andExpect(status().isCreated());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
            "/api/work-efforts/role-type-assignments"
        )
            .param("tenantCode", "workwebrole02")
            .param("effortNumber", "we-role-001")
            .param("roleTypeCode", "task-master")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(2));
    }

    @Test
    void rejectsWorkEffortRoleTypeAssignmentForMissingWorkEffort() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
            "/api/work-efforts/role-type-assignments"
        )
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {
                  "tenantCode": "workwebrole03",
                  "effortNumber": "missing",
                  "roleTypeCode": "task-master"
                }
                """))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value(
                "Work effort not found for tenant/effortNumber: WORKWEBROLE03/MISSING"
            ))
            .andExpect(jsonPath("$.path").value("/api/work-efforts/role-type-assignments"));
    }

    @Test
    void rejectsUnknownAssignee() throws Exception {
        WorkEffortsWebIntegrationTestSupport.createWorkEffort(
            mockMvc,
            "workweb04",
            "we-001",
            "Prepare shipment",
            "Prepare shipment for dispatch",
            "PLANNED",
            "missing@work.com",
            null
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("work effort assignee not found in tenant: WORKWEB04/missing@work.com"))
            .andExpect(jsonPath("$.path").value("/api/work-efforts"));
    }

    @Test
    void returnsNotFoundForMissingWorkEffort() throws Exception {
        mockMvc.perform(WorkEffortsWebIntegrationTestSupport.getWorkEffortRequest("workweb05", "missing"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("Work effort not found for tenant/effortNumber: WORKWEB05/MISSING"))
            .andExpect(jsonPath("$.path").value("/api/work-efforts/missing"));
    }

    @Test
    void changesStatusAndReadsStatusHistory() throws Exception {
        ActorActivationWebTestSupport.registerActorAllowingDuplicateEmail(
            mockMvc,
            "workweb07",
            "agent01@work.com",
            "Work Web",
            "Agent 01"
        );

        WorkEffortsWebIntegrationTestSupport.createWorkEffort(
            mockMvc,
            "workweb07",
            "we-001",
            "Prepare shipment",
            "Prepare shipment for dispatch",
            "PLANNED",
            "agent01@work.com",
            null
        )
            .andExpect(status().isCreated());

        WorkEffortsWebIntegrationTestSupport.changeWorkEffortStatus(
            mockMvc,
            "workweb07",
            "we-001",
            "IN_PROGRESS",
            "Started picking",
            "AGENT01@WORK.COM"
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        WorkEffortsWebIntegrationTestSupport.changeWorkEffortStatus(
            mockMvc,
            "workweb07",
            "we-001",
            "IN_PROGRESS",
            "No-op status change",
            "agent01@work.com"
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        mockMvc.perform(
            WorkEffortsWebIntegrationTestSupport.workEffortStatusHistoryRequest(
                "workweb07",
                "we-001",
                0,
                10,
                "changedBy", "agent01@work.com"
            )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].previousStatus").value("PLANNED"))
            .andExpect(jsonPath("$.items[0].currentStatus").value("IN_PROGRESS"));
    }

    @Test
    void rejectsUnknownStatusActor() throws Exception {
        ActorActivationWebTestSupport.registerActorAllowingDuplicateEmail(
            mockMvc,
            "workweb08",
            "agent01@work.com",
            "Work Web",
            "Agent 01"
        );

        WorkEffortsWebIntegrationTestSupport.createWorkEffort(
            mockMvc,
            "workweb08",
            "we-001",
            "Prepare shipment",
            "Prepare shipment for dispatch",
            "PLANNED",
            "agent01@work.com",
            null
        )
            .andExpect(status().isCreated());

        WorkEffortsWebIntegrationTestSupport.changeWorkEffortStatus(
            mockMvc,
            "workweb08",
            "we-001",
            "IN_PROGRESS",
            "Started picking",
            "missing@work.com"
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("work effort status actor not found in tenant: WORKWEB08/missing@work.com"));
    }

    @Test
    void changesAssignmentAndReadsAssignmentHistory() throws Exception {
        ActorActivationWebTestSupport.registerActorAllowingDuplicateEmail(
            mockMvc,
            "workweb09",
            "agent01@work.com",
            "Work Web",
            "Agent 01"
        );
        ActorActivationWebTestSupport.registerActorAllowingDuplicateEmail(
            mockMvc,
            "workweb09",
            "agent02@work.com",
            "Work Web",
            "Agent 02"
        );
        ActorActivationWebTestSupport.registerActorAllowingDuplicateEmail(
            mockMvc,
            "workweb09",
            "manager@work.com",
            "Work Web",
            "Manager"
        );

        WorkEffortsWebIntegrationTestSupport.createWorkEffort(
            mockMvc,
            "workweb09",
            "we-001",
            "Prepare shipment",
            "Prepare shipment for dispatch",
            "PLANNED",
            "agent01@work.com",
            null
        )
            .andExpect(status().isCreated());

        WorkEffortsWebIntegrationTestSupport.assignWorkEffort(
            mockMvc,
            "workweb09",
            "we-001",
            "AGENT02@WORK.COM",
            "Coverage handoff",
            "MANAGER@WORK.COM"
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.assignedTo").value("agent02@work.com"));

        WorkEffortsWebIntegrationTestSupport.assignWorkEffort(
            mockMvc,
            "workweb09",
            "we-001",
            "agent02@work.com",
            "No-op assignment",
            "manager@work.com"
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.assignedTo").value("agent02@work.com"));

        mockMvc.perform(WorkEffortsWebIntegrationTestSupport.getWorkEffortAssignmentRequest("workweb09", "we-001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tenantCode").value("WORKWEB09"))
            .andExpect(jsonPath("$.effortNumber").value("WE-001"))
            .andExpect(jsonPath("$.assignedTo").value("agent02@work.com"));

        mockMvc.perform(
            WorkEffortsWebIntegrationTestSupport.workEffortAssignmentHistoryRequest(
                "workweb09",
                "we-001",
                0,
                10,
                "assignedTo", "agent02@work.com",
                "assignedBy", "manager@work.com"
            )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].previousAssignedTo").value("agent01@work.com"))
            .andExpect(jsonPath("$.items[0].currentAssignedTo").value("agent02@work.com"))
            .andExpect(jsonPath("$.items[0].assignedBy").value("manager@work.com"));
    }

    @Test
    void filtersAssignmentHistoryAtWebBoundary() throws Exception {
        ActorActivationWebTestSupport.registerActorAllowingDuplicateEmail(
            mockMvc,
            "workweb12",
            "agent01@work.com",
            "Work Web",
            "Agent 01"
        );
        ActorActivationWebTestSupport.registerActorAllowingDuplicateEmail(
            mockMvc,
            "workweb12",
            "agent02@work.com",
            "Work Web",
            "Agent 02"
        );
        ActorActivationWebTestSupport.registerActorAllowingDuplicateEmail(
            mockMvc,
            "workweb12",
            "agent03@work.com",
            "Work Web",
            "Agent 03"
        );
        ActorActivationWebTestSupport.registerActorAllowingDuplicateEmail(
            mockMvc,
            "workweb12",
            "manager@work.com",
            "Work Web",
            "Manager"
        );
        ActorActivationWebTestSupport.registerActorAllowingDuplicateEmail(
            mockMvc,
            "workweb12",
            "lead@work.com",
            "Work Web",
            "Lead"
        );

        WorkEffortsWebIntegrationTestSupport.createWorkEffort(
            mockMvc,
            "workweb12",
            "we-001",
            "Prepare shipment",
            "Prepare shipment for dispatch",
            "PLANNED",
            "agent01@work.com",
            null
        )
            .andExpect(status().isCreated());

        WorkEffortsWebIntegrationTestSupport.assignWorkEffort(
            mockMvc,
            "workweb12",
            "we-001",
            "AGENT02@WORK.COM",
            "Coverage handoff",
            "MANAGER@WORK.COM"
        )
            .andExpect(status().isOk());
        WorkEffortsWebIntegrationTestSupport.assignWorkEffort(
            mockMvc,
            "workweb12",
            "we-001",
            "AGENT03@WORK.COM",
            "Escalation",
            "LEAD@WORK.COM"
        )
            .andExpect(status().isOk());

        MvcResult allHistory = mockMvc.perform(
            WorkEffortsWebIntegrationTestSupport.workEffortAssignmentHistoryRequest("workweb12", "we-001", 0, 10)
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(2))
            .andReturn();
        JsonNode newestAssignment = objectMapper.readTree(allHistory.getResponse().getContentAsString()).path("items").get(0);
        String newestAssignedAt = newestAssignment.path("assignedAt").asText();

        mockMvc.perform(
            WorkEffortsWebIntegrationTestSupport.workEffortAssignmentHistoryRequest(
                "workweb12",
                "we-001",
                0,
                10,
                "assignedTo", "AGENT02@WORK.COM"
            )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].currentAssignedTo").value("agent02@work.com"));

        mockMvc.perform(
            WorkEffortsWebIntegrationTestSupport.workEffortAssignmentHistoryRequest(
                "workweb12",
                "we-001",
                0,
                10,
                "assignedBy", "LEAD@WORK.COM"
            )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].assignedBy").value("lead@work.com"));

        mockMvc.perform(
            WorkEffortsWebIntegrationTestSupport.workEffortAssignmentHistoryRequest(
                "workweb12",
                "we-001",
                0,
                10,
                "assignedAtFrom", newestAssignedAt,
                "assignedAtTo", newestAssignedAt
            )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[*].currentAssignedTo").value(hasItem(newestAssignment.path("currentAssignedTo").asText())))
            .andExpect(jsonPath("$.items[*].assignedBy").value(hasItem(newestAssignment.path("assignedBy").asText())));
    }

    @Test
    void readsAssignmentActivitySummaryAtWebBoundary() throws Exception {
        ActorActivationWebTestSupport.registerActorAllowingDuplicateEmail(
            mockMvc,
            "workweb13",
            "agent01@work.com",
            "Work Web",
            "Agent 01"
        );
        ActorActivationWebTestSupport.registerActorAllowingDuplicateEmail(
            mockMvc,
            "workweb13",
            "agent02@work.com",
            "Work Web",
            "Agent 02"
        );
        ActorActivationWebTestSupport.registerActorAllowingDuplicateEmail(
            mockMvc,
            "workweb13",
            "agent03@work.com",
            "Work Web",
            "Agent 03"
        );
        ActorActivationWebTestSupport.registerActorAllowingDuplicateEmail(
            mockMvc,
            "workweb13",
            "manager@work.com",
            "Work Web",
            "Manager"
        );

        WorkEffortsWebIntegrationTestSupport.createWorkEffort(
            mockMvc,
            "workweb13",
            "we-001",
            "Prepare shipment",
            "Prepare shipment for dispatch",
            "PLANNED",
            "agent01@work.com",
            null
        )
            .andExpect(status().isCreated());

        WorkEffortsWebIntegrationTestSupport.assignWorkEffort(
            mockMvc,
            "workweb13",
            "we-001",
            "AGENT02@WORK.COM",
            "Coverage handoff",
            "manager@work.com"
        )
            .andExpect(status().isOk());
        WorkEffortsWebIntegrationTestSupport.assignWorkEffort(
            mockMvc,
            "workweb13",
            "we-001",
            "AGENT03@WORK.COM",
            "Escalation",
            "manager@work.com"
        )
            .andExpect(status().isOk());

        mockMvc.perform(
            WorkEffortsWebIntegrationTestSupport.workEffortAssignmentActivitySummaryRequest(
                "workweb13",
                0,
                10,
                "assignedTo", "AGENT03@WORK.COM"
            )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].tenantCode").value("WORKWEB13"))
            .andExpect(jsonPath("$.items[0].assignedTo").value("agent03@work.com"))
            .andExpect(jsonPath("$.items[0].assignmentCount").value(1))
            .andExpect(jsonPath("$.items[0].firstAssignedAt").exists())
            .andExpect(jsonPath("$.items[0].lastAssignedAt").exists());
    }

    @Test
    void readsDailyWeeklyAndMonthlyAssignmentActivitySummariesAtWebBoundary() throws Exception {
        ActorActivationWebTestSupport.registerActorAllowingDuplicateEmail(
            mockMvc,
            "workweb14",
            "agent01@work.com",
            "Work Web",
            "Agent 01"
        );
        ActorActivationWebTestSupport.registerActorAllowingDuplicateEmail(
            mockMvc,
            "workweb14",
            "agent02@work.com",
            "Work Web",
            "Agent 02"
        );
        ActorActivationWebTestSupport.registerActorAllowingDuplicateEmail(
            mockMvc,
            "workweb14",
            "agent03@work.com",
            "Work Web",
            "Agent 03"
        );
        ActorActivationWebTestSupport.registerActorAllowingDuplicateEmail(
            mockMvc,
            "workweb14",
            "manager@work.com",
            "Work Web",
            "Manager"
        );

        WorkEffortsWebIntegrationTestSupport.createWorkEffort(
            mockMvc,
            "workweb14",
            "we-001",
            "Prepare shipment",
            "Prepare shipment for dispatch",
            "PLANNED",
            "agent01@work.com",
            null
        )
            .andExpect(status().isCreated());
        WorkEffortsWebIntegrationTestSupport.createWorkEffort(
            mockMvc,
            "workweb14",
            "we-002",
            "Confirm receipt",
            "Confirm inbound receipt",
            "PLANNED",
            "agent01@work.com",
            null
        )
            .andExpect(status().isCreated());

        testClock.setInstant(Instant.parse("2026-04-22T10:00:00Z"));
        WorkEffortsWebIntegrationTestSupport.assignWorkEffort(
            mockMvc,
            "workweb14",
            "we-001",
            "agent02@work.com",
            "Coverage handoff",
            "manager@work.com"
        )
            .andExpect(status().isOk());
        testClock.setInstant(Instant.parse("2026-04-23T11:00:00Z"));
        WorkEffortsWebIntegrationTestSupport.assignWorkEffort(
            mockMvc,
            "workweb14",
            "we-001",
            "agent03@work.com",
            "Escalation",
            "manager@work.com"
        )
            .andExpect(status().isOk());
        testClock.setInstant(Instant.parse("2026-05-04T12:00:00Z"));
        WorkEffortsWebIntegrationTestSupport.assignWorkEffort(
            mockMvc,
            "workweb14",
            "we-002",
            "agent03@work.com",
            "Month handoff",
            "manager@work.com"
        )
            .andExpect(status().isOk());

        mockMvc.perform(
            WorkEffortsWebIntegrationTestSupport.dailyWorkEffortAssignmentActivitySummaryRequest("workweb14", 0, 10)
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(3))
            .andExpect(jsonPath("$.items[0].businessDate").value("2026-05-04"))
            .andExpect(jsonPath("$.items[0].assignmentCount").value(1))
            .andExpect(jsonPath("$.items[1].businessDate").value("2026-04-23"))
            .andExpect(jsonPath("$.items[2].businessDate").value("2026-04-22"));

        mockMvc.perform(
            WorkEffortsWebIntegrationTestSupport.weeklyWorkEffortAssignmentActivitySummaryRequest("workweb14", 0, 10)
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.items[0].businessWeekStart").value("2026-05-04"))
            .andExpect(jsonPath("$.items[1].businessWeekStart").value("2026-04-20"))
            .andExpect(jsonPath("$.items[1].assignmentCount").value(2))
            .andExpect(jsonPath("$.items[1].workEffortCount").value(1));

        mockMvc.perform(
            WorkEffortsWebIntegrationTestSupport.monthlyWorkEffortAssignmentActivitySummaryRequest(
                "workweb14",
                0,
                10,
                "assignedTo", "AGENT03@WORK.COM",
                "assignedAtFrom", "2026-04-23T00:00:00Z",
                "assignedAtTo", "2026-05-04T23:59:59Z"
            )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.items[0].businessMonth").value("2026-05"))
            .andExpect(jsonPath("$.items[1].businessMonth").value("2026-04"));

        mockMvc.perform(
            WorkEffortsWebIntegrationTestSupport.dailyWorkEffortAssignmentActivityByAssigneeSummaryRequest("workweb14", 0, 10)
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(3))
            .andExpect(jsonPath("$.items[0].businessDate").value("2026-05-04"))
            .andExpect(jsonPath("$.items[0].assignedTo").value("agent03@work.com"))
            .andExpect(jsonPath("$.items[1].businessDate").value("2026-04-23"))
            .andExpect(jsonPath("$.items[1].assignedTo").value("agent03@work.com"))
            .andExpect(jsonPath("$.items[2].businessDate").value("2026-04-22"))
            .andExpect(jsonPath("$.items[2].assignedTo").value("agent02@work.com"));

        mockMvc.perform(
            WorkEffortsWebIntegrationTestSupport.weeklyWorkEffortAssignmentActivityByAssigneeSummaryRequest("workweb14", 0, 10)
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(3))
            .andExpect(jsonPath("$.items[0].businessWeekStart").value("2026-05-04"))
            .andExpect(jsonPath("$.items[0].assignedTo").value("agent03@work.com"))
            .andExpect(jsonPath("$.items[1].businessWeekStart").value("2026-04-20"))
            .andExpect(jsonPath("$.items[1].assignedTo").value("agent02@work.com"))
            .andExpect(jsonPath("$.items[2].assignedTo").value("agent03@work.com"));

        mockMvc.perform(
            WorkEffortsWebIntegrationTestSupport.monthlyWorkEffortAssignmentActivityByAssigneeSummaryRequest(
                "workweb14",
                0,
                1,
                "assignedTo", "AGENT03@WORK.COM",
                "assignedAtFrom", "2026-04-01T00:00:00Z",
                "assignedAtTo", "2026-05-31T23:59:59Z"
            )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.items[0].businessMonth").value("2026-05"))
            .andExpect(jsonPath("$.items[0].assignedTo").value("agent03@work.com"))
            .andExpect(jsonPath("$.hasNext").value(true));
    }

    @Test
    void rejectsUnknownAssignmentAssignee() throws Exception {
        ActorActivationWebTestSupport.registerActorAllowingDuplicateEmail(
            mockMvc,
            "workweb10",
            "agent01@work.com",
            "Work Web",
            "Agent 01"
        );
        ActorActivationWebTestSupport.registerActorAllowingDuplicateEmail(
            mockMvc,
            "workweb10",
            "manager@work.com",
            "Work Web",
            "Manager"
        );

        WorkEffortsWebIntegrationTestSupport.createWorkEffort(
            mockMvc,
            "workweb10",
            "we-001",
            "Prepare shipment",
            "Prepare shipment for dispatch",
            "PLANNED",
            "agent01@work.com",
            null
        )
            .andExpect(status().isCreated());

        WorkEffortsWebIntegrationTestSupport.assignWorkEffort(
            mockMvc,
            "workweb10",
            "we-001",
            "missing@work.com",
            "Coverage handoff",
            "manager@work.com"
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("work effort assignee not found in tenant: WORKWEB10/missing@work.com"))
            .andExpect(jsonPath("$.path").value("/api/work-efforts/we-001/assignment"));
    }

    @Test
    void rejectsUnknownAssignmentActor() throws Exception {
        ActorActivationWebTestSupport.registerActorAllowingDuplicateEmail(
            mockMvc,
            "workweb11",
            "agent01@work.com",
            "Work Web",
            "Agent 01"
        );
        ActorActivationWebTestSupport.registerActorAllowingDuplicateEmail(
            mockMvc,
            "workweb11",
            "agent02@work.com",
            "Work Web",
            "Agent 02"
        );

        WorkEffortsWebIntegrationTestSupport.createWorkEffort(
            mockMvc,
            "workweb11",
            "we-001",
            "Prepare shipment",
            "Prepare shipment for dispatch",
            "PLANNED",
            "agent01@work.com",
            null
        )
            .andExpect(status().isCreated());

        WorkEffortsWebIntegrationTestSupport.assignWorkEffort(
            mockMvc,
            "workweb11",
            "we-001",
            "agent02@work.com",
            "Coverage handoff",
            "missing-manager@work.com"
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("work effort assignment actor not found in tenant: WORKWEB11/missing-manager@work.com"))
            .andExpect(jsonPath("$.path").value("/api/work-efforts/we-001/assignment"));
    }

    @Test
    void rejectsInvalidFiltersAndPagination() throws Exception {
        mockMvc.perform(
            WorkEffortsWebIntegrationTestSupport.listWorkEffortsRequest("workweb06", 0, 10, "assignedTo", "   ")
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("assignedTo query parameter must not be blank"));

        mockMvc.perform(WorkEffortsWebIntegrationTestSupport.listWorkEffortsRequest("workweb06", -1, 10))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("page must be greater than or equal to zero"));

        mockMvc.perform(
            WorkEffortsWebIntegrationTestSupport.workEffortStatusHistoryRequest(
                "workweb06",
                "we-001",
                0,
                10,
                "changedBy", "   "
            )
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("assignedTo query parameter must not be blank"));

        mockMvc.perform(
            WorkEffortsWebIntegrationTestSupport.workEffortStatusHistoryRequest(
                "workweb06",
                "we-001",
                0,
                10,
                "changedAtFrom", "not-a-timestamp"
            )
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("changedAtFrom query parameter must be a valid ISO-8601 instant"));

        mockMvc.perform(
            WorkEffortsWebIntegrationTestSupport.workEffortStatusHistoryRequest(
                "workweb06",
                "we-001",
                0,
                10,
                "changedAtFrom", "2026-04-23T00:00:00Z",
                "changedAtTo", "2026-04-22T00:00:00Z"
            )
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("changedAtFrom must be before or equal to changedAtTo"));

        mockMvc.perform(
            WorkEffortsWebIntegrationTestSupport.workEffortAssignmentHistoryRequest(
                "workweb06",
                "we-001",
                0,
                10,
                "assignedBy", "   "
            )
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("assignedBy query parameter must not be blank"));

        mockMvc.perform(
            WorkEffortsWebIntegrationTestSupport.workEffortAssignmentHistoryRequest(
                "workweb06",
                "we-001",
                0,
                10,
                "assignedAtFrom", "2026-04-23T00:00:00Z",
                "assignedAtTo", "2026-04-22T00:00:00Z"
            )
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("assignedAtFrom must be before or equal to assignedAtTo"));
    }
}
