package com.arcanaerp.platform.agreements.web;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.arcanaerp.platform.agreements.AgreementStatus;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@Import(AgreementsDeterministicClockTestSupport.Configuration.class)
class AgreementsControllerIntegrationTest {

    private static final String AGREEMENTS_TENANT_CODE = "TENAGR01";
    private static final String AGREEMENTS_ALT_TENANT_CODE = "TENAGR02";
    private static final String UNKNOWN_TENANT_CODE = "TENAGR99";
    private static final String MISMATCH_ACTOR_TENANT_CODE = "TENAGR11";
    private static final String MISMATCH_REQUEST_TENANT_CODE = "TENAGR12";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AgreementsDeterministicClockTestSupport.AdjustableClock testClock;

    @BeforeEach
    void resetClock() {
        testClock.resetToBaseInstant();
    }

    @Test
    void createsAndReadsAgreementType() throws Exception {
        String payload = """
            {
              "parentId": 42,
              "left": 3,
              "right": 4,
              "description": "  Service Agreement Type ",
              "comments": "Legacy agreement type catalog entry",
              "internalIdentifier": " service-agreement ",
              "externalIdentifier": " EXT-AGR-TYPE-1 ",
              "externalIdSource": " legacy "
            }
            """;

        MvcResult result = mockMvc.perform(post("/api/agreements/types")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.parentId").value(42))
            .andExpect(jsonPath("$.left").value(3))
            .andExpect(jsonPath("$.right").value(4))
            .andExpect(jsonPath("$.description").value("Service Agreement Type"))
            .andExpect(jsonPath("$.comments").value("Legacy agreement type catalog entry"))
            .andExpect(jsonPath("$.internalIdentifier").value("service-agreement"))
            .andExpect(jsonPath("$.externalIdentifier").value("EXT-AGR-TYPE-1"))
            .andExpect(jsonPath("$.externalIdSource").value("legacy"))
            .andExpect(jsonPath("$.createdAt").isNotEmpty())
            .andReturn();

        String id = com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/agreements/types/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$.description").value("Service Agreement Type"));

        mockMvc.perform(get("/api/agreements/types")
            .param("parentId", "42")
            .param("internalIdentifier", "service-agreement")
            .param("externalIdentifier", "EXT-AGR-TYPE-1")
            .param("externalIdSource", "legacy")
            .param("page", "0")
            .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[?(@.id=='" + id + "')].description", hasItem("Service Agreement Type")));
    }

    @Test
    void createsAndReadsAgreementItemTypeAndAgreementItem() throws Exception {
        MvcResult typeResult = mockMvc.perform(post("/api/agreements/item-types")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "parentId": 17,
                  "left": 5,
                  "right": 6,
                  "description": "  Reference Number ",
                  "comments": "Legacy item type",
                  "internalIdentifier": " reference_number ",
                  "externalIdentifier": " EXT-ITEM-TYPE-1 ",
                  "externalIdSource": " legacy "
                }
                """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.parentId").value(17))
            .andExpect(jsonPath("$.left").value(5))
            .andExpect(jsonPath("$.right").value(6))
            .andExpect(jsonPath("$.description").value("Reference Number"))
            .andExpect(jsonPath("$.internalIdentifier").value("reference_number"))
            .andReturn();
        String itemTypeId = com.jayway.jsonpath.JsonPath.read(
            typeResult.getResponse().getContentAsString(),
            "$.id"
        );

        mockMvc.perform(get("/api/agreements/item-types/{id}", itemTypeId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(itemTypeId))
            .andExpect(jsonPath("$.externalIdentifier").value("EXT-ITEM-TYPE-1"));

        mockMvc.perform(get("/api/agreements/item-types")
            .param("parentId", "17")
            .param("internalIdentifier", "reference_number")
            .param("page", "0")
            .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[?(@.id=='" + itemTypeId + "')].description", hasItem("Reference Number")));

        MvcResult agreementResult = mockMvc.perform(post("/api/agreements")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "tenantCode": "tenant-agreement-items",
                  "agreementNumber": "agr-item-1",
                  "name": "Agreement Item Parent",
                  "agreementType": "service",
                  "effectiveFrom": "2026-03-01T00:00:00Z"
                }
                """))
            .andExpect(status().isCreated())
            .andReturn();
        String agreementId = com.jayway.jsonpath.JsonPath.read(
            agreementResult.getResponse().getContentAsString(),
            "$.id"
        );

        MvcResult itemResult = mockMvc.perform(post("/api/agreements/items")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "agreementId": "%s",
                  "agreementItemTypeId": "%s",
                  "agreementItemValue": " 1234ABCD ",
                  "description": " Customer reference ",
                  "agreementItemRuleString": " value != null "
                }
                """.formatted(agreementId, itemTypeId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.agreementId").value(agreementId))
            .andExpect(jsonPath("$.agreementItemTypeId").value(itemTypeId))
            .andExpect(jsonPath("$.agreementItemValue").value("1234ABCD"))
            .andExpect(jsonPath("$.description").value("Customer reference"))
            .andExpect(jsonPath("$.agreementItemRuleString").value("value != null"))
            .andReturn();
        String itemId = com.jayway.jsonpath.JsonPath.read(itemResult.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/agreements/items/{id}", itemId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(itemId))
            .andExpect(jsonPath("$.agreementItemValue").value("1234ABCD"));

        mockMvc.perform(get("/api/agreements/items")
            .param("agreementId", agreementId)
            .param("agreementItemTypeId", itemTypeId)
            .param("page", "0")
            .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[?(@.id=='" + itemId + "')].agreementItemValue", hasItem("1234ABCD")));
    }

    @Test
    void allowsEmptyAgreementItemTypeAndAgreementItemForLegacyParity() throws Exception {
        mockMvc.perform(post("/api/agreements/item-types")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.description").value(nullValue()));

        mockMvc.perform(post("/api/agreements/items")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.agreementId").value(nullValue()))
            .andExpect(jsonPath("$.agreementItemTypeId").value(nullValue()))
            .andExpect(jsonPath("$.agreementItemValue").value(nullValue()));
    }

    @Test
    void createsAndReadsAgreementRoleTypeAndPartyRole() throws Exception {
        MvcResult roleTypeResult = mockMvc.perform(post("/api/agreements/role-types")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "parentId": 23,
                  "left": 7,
                  "right": 8,
                  "description": "  Agreement Signatory ",
                  "comments": "Legacy agreement role type",
                  "internalIdentifier": " agreement_signatory ",
                  "externalIdentifier": " EXT-AGR-ROLE-1 ",
                  "externalIdSource": " legacy "
                }
                """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.parentId").value(23))
            .andExpect(jsonPath("$.left").value(7))
            .andExpect(jsonPath("$.right").value(8))
            .andExpect(jsonPath("$.description").value("Agreement Signatory"))
            .andExpect(jsonPath("$.internalIdentifier").value("agreement_signatory"))
            .andReturn();
        String agreementRoleTypeId = com.jayway.jsonpath.JsonPath.read(
            roleTypeResult.getResponse().getContentAsString(),
            "$.id"
        );

        mockMvc.perform(get("/api/agreements/role-types/{id}", agreementRoleTypeId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(agreementRoleTypeId))
            .andExpect(jsonPath("$.externalIdentifier").value("EXT-AGR-ROLE-1"));

        mockMvc.perform(get("/api/agreements/role-types")
            .param("parentId", "23")
            .param("internalIdentifier", "agreement_signatory")
            .param("page", "0")
            .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath(
                "$.items[?(@.id=='" + agreementRoleTypeId + "')].description",
                hasItem("Agreement Signatory")
            ));

        MvcResult agreementResult = mockMvc.perform(post("/api/agreements")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "tenantCode": "tenant-agreement-party-roles",
                  "agreementNumber": "agr-party-role-1",
                  "name": "Agreement Party Role Parent",
                  "agreementType": "service",
                  "effectiveFrom": "2026-03-01T00:00:00Z"
                }
                """))
            .andExpect(status().isCreated())
            .andReturn();
        String agreementId = com.jayway.jsonpath.JsonPath.read(
            agreementResult.getResponse().getContentAsString(),
            "$.id"
        );

        MvcResult partyRoleResult = mockMvc.perform(post("/api/agreements/party-roles")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "description": " Customer signatory ",
                  "agreementId": "%s",
                  "partyId": 401,
                  "roleTypeId": 77,
                  "externalIdentifier": " EXT-PARTY-ROLE-1 ",
                  "externalIdSource": " legacy "
                }
                """.formatted(agreementId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.description").value("Customer signatory"))
            .andExpect(jsonPath("$.agreementId").value(agreementId))
            .andExpect(jsonPath("$.partyId").value(401))
            .andExpect(jsonPath("$.roleTypeId").value(77))
            .andExpect(jsonPath("$.externalIdentifier").value("EXT-PARTY-ROLE-1"))
            .andReturn();
        String partyRoleId = com.jayway.jsonpath.JsonPath.read(
            partyRoleResult.getResponse().getContentAsString(),
            "$.id"
        );

        mockMvc.perform(get("/api/agreements/party-roles/{id}", partyRoleId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(partyRoleId))
            .andExpect(jsonPath("$.partyId").value(401));

        mockMvc.perform(get("/api/agreements/party-roles")
            .param("agreementId", agreementId)
            .param("partyId", "401")
            .param("roleTypeId", "77")
            .param("externalIdentifier", "EXT-PARTY-ROLE-1")
            .param("externalIdSource", "legacy")
            .param("page", "0")
            .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath(
                "$.items[?(@.id=='" + partyRoleId + "')].description",
                hasItem("Customer signatory")
            ));
    }

    @Test
    void allowsEmptyAgreementRoleTypeAndPartyRoleForLegacyParity() throws Exception {
        mockMvc.perform(post("/api/agreements/role-types")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.description").value(nullValue()));

        mockMvc.perform(post("/api/agreements/party-roles")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.agreementId").value(nullValue()))
            .andExpect(jsonPath("$.partyId").value(nullValue()))
            .andExpect(jsonPath("$.roleTypeId").value(nullValue()));
    }

    @Test
    void createsAndReadsAgreementRelationshipTypeAndRelationship() throws Exception {
        String sellerRoleTypeId = createAgreementRoleType("seller", "Seller");
        String buyerRoleTypeId = createAgreementRoleType("buyer", "Buyer");

        MvcResult relationshipTypeResult = mockMvc.perform(post("/api/agreements/relationship-types")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "parentId": 31,
                  "left": 9,
                  "right": 10,
                  "validFromRoleTypeId": "%s",
                  "validToRoleTypeId": "%s",
                  "name": " Buyer Seller ",
                  "description": " Buyer seller agreement relationship ",
                  "internalIdentifier": " buyer_seller ",
                  "externalIdentifier": " EXT-RELN-TYPE-1 ",
                  "externalIdSource": " legacy "
                }
                """.formatted(sellerRoleTypeId, buyerRoleTypeId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.parentId").value(31))
            .andExpect(jsonPath("$.left").value(9))
            .andExpect(jsonPath("$.right").value(10))
            .andExpect(jsonPath("$.validFromRoleTypeId").value(sellerRoleTypeId))
            .andExpect(jsonPath("$.validToRoleTypeId").value(buyerRoleTypeId))
            .andExpect(jsonPath("$.name").value("Buyer Seller"))
            .andExpect(jsonPath("$.internalIdentifier").value("buyer_seller"))
            .andReturn();
        String relationshipTypeId = com.jayway.jsonpath.JsonPath.read(
            relationshipTypeResult.getResponse().getContentAsString(),
            "$.id"
        );

        mockMvc.perform(get("/api/agreements/relationship-types/{id}", relationshipTypeId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(relationshipTypeId))
            .andExpect(jsonPath("$.externalIdentifier").value("EXT-RELN-TYPE-1"));

        mockMvc.perform(get("/api/agreements/relationship-types")
            .param("parentId", "31")
            .param("validFromRoleTypeId", sellerRoleTypeId)
            .param("validToRoleTypeId", buyerRoleTypeId)
            .param("internalIdentifier", "buyer_seller")
            .param("page", "0")
            .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath(
                "$.items[?(@.id=='" + relationshipTypeId + "')].name",
                hasItem("Buyer Seller")
            ));

        String agreementIdFrom = createAgreementAndReturnId(
            "tenant-agreement-relationships",
            "agr-reln-from-1",
            "Relationship From Agreement"
        );
        String agreementIdTo = createAgreementAndReturnId(
            "tenant-agreement-relationships",
            "agr-reln-to-1",
            "Relationship To Agreement"
        );

        MvcResult relationshipResult = mockMvc.perform(post("/api/agreements/relationships")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "agreementRelationshipTypeId": "%s",
                  "description": " Buyer seller link ",
                  "agreementIdFrom": "%s",
                  "agreementIdTo": "%s",
                  "roleTypeIdFrom": "%s",
                  "roleTypeIdTo": "%s",
                  "statusTypeId": 91,
                  "fromDate": "2026-05-01",
                  "thruDate": "2027-04-30"
                }
                """.formatted(
                    relationshipTypeId,
                    agreementIdFrom,
                    agreementIdTo,
                    sellerRoleTypeId,
                    buyerRoleTypeId
                )))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.agreementRelationshipTypeId").value(relationshipTypeId))
            .andExpect(jsonPath("$.description").value("Buyer seller link"))
            .andExpect(jsonPath("$.agreementIdFrom").value(agreementIdFrom))
            .andExpect(jsonPath("$.agreementIdTo").value(agreementIdTo))
            .andExpect(jsonPath("$.roleTypeIdFrom").value(sellerRoleTypeId))
            .andExpect(jsonPath("$.roleTypeIdTo").value(buyerRoleTypeId))
            .andExpect(jsonPath("$.statusTypeId").value(91))
            .andExpect(jsonPath("$.fromDate").value("2026-05-01"))
            .andExpect(jsonPath("$.thruDate").value("2027-04-30"))
            .andReturn();
        String relationshipId = com.jayway.jsonpath.JsonPath.read(
            relationshipResult.getResponse().getContentAsString(),
            "$.id"
        );

        mockMvc.perform(get("/api/agreements/relationships/{id}", relationshipId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(relationshipId))
            .andExpect(jsonPath("$.description").value("Buyer seller link"));

        mockMvc.perform(get("/api/agreements/relationships")
            .param("agreementRelationshipTypeId", relationshipTypeId)
            .param("agreementIdFrom", agreementIdFrom)
            .param("agreementIdTo", agreementIdTo)
            .param("roleTypeIdFrom", sellerRoleTypeId)
            .param("roleTypeIdTo", buyerRoleTypeId)
            .param("statusTypeId", "91")
            .param("fromDate", "2026-05-01")
            .param("thruDate", "2027-04-30")
            .param("page", "0")
            .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath(
                "$.items[?(@.id=='" + relationshipId + "')].description",
                hasItem("Buyer seller link")
            ));
    }

    @Test
    void allowsEmptyAgreementRelationshipTypeAndRelationshipForLegacyParity() throws Exception {
        mockMvc.perform(post("/api/agreements/relationship-types")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.validFromRoleTypeId").value(nullValue()))
            .andExpect(jsonPath("$.validToRoleTypeId").value(nullValue()));

        mockMvc.perform(post("/api/agreements/relationships")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.agreementRelationshipTypeId").value(nullValue()))
            .andExpect(jsonPath("$.agreementIdFrom").value(nullValue()))
            .andExpect(jsonPath("$.agreementIdTo").value(nullValue()))
            .andExpect(jsonPath("$.fromDate").value(nullValue()))
            .andExpect(jsonPath("$.thruDate").value(nullValue()));
    }

    @Test
    void createsAndReadsLoyaltyProgramCode() throws Exception {
        String payload = """
            {
              "identifier": " loyalty-gold ",
              "name": " Gold Loyalty Program ",
              "description": " Preferred customer benefits "
            }
            """;

        MvcResult result = mockMvc.perform(post("/api/agreements/loyalty-program-codes")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.identifier").value("loyalty-gold"))
            .andExpect(jsonPath("$.name").value("Gold Loyalty Program"))
            .andExpect(jsonPath("$.description").value("Preferred customer benefits"))
            .andExpect(jsonPath("$.createdAt").isNotEmpty())
            .andReturn();
        String id = com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/agreements/loyalty-program-codes/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$.identifier").value("loyalty-gold"));

        mockMvc.perform(get("/api/agreements/loyalty-program-codes")
            .param("identifier", "loyalty-gold")
            .param("name", "Gold Loyalty Program")
            .param("page", "0")
            .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath(
                "$.items[?(@.id=='" + id + "')].description",
                hasItem("Preferred customer benefits")
            ));

        mockMvc.perform(post("/api/agreements/loyalty-program-codes")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
            .andExpect(status().isCreated());
    }

    @Test
    void allowsEmptyLoyaltyProgramCodeForLegacyParity() throws Exception {
        mockMvc.perform(post("/api/agreements/loyalty-program-codes")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.identifier").value(nullValue()))
            .andExpect(jsonPath("$.name").value(nullValue()))
            .andExpect(jsonPath("$.description").value(nullValue()));
    }

    @Test
    void createsAgreement() throws Exception {
        String payload = """
            {
              "tenantCode": " tenant-agreements ",
              "agreementNumber": " agr-3000 ",
              "name": "  Master Services Agreement ",
              "agreementType": "service",
              "effectiveFrom": "2026-03-01T00:00:00Z"
            }
            """;

        mockMvc.perform(post("/api/agreements")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tenantCode").value("TENANT-AGREEMENTS"))
            .andExpect(jsonPath("$.agreementNumber").value("AGR-3000"))
            .andExpect(jsonPath("$.name").value("Master Services Agreement"))
            .andExpect(jsonPath("$.agreementType").value("SERVICE"))
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.effectiveFrom").value("2026-03-01T00:00:00Z"))
            .andExpect(jsonPath("$.activatedAt").value(nullValue()))
            .andExpect(jsonPath("$.terminatedAt").value(nullValue()))
            .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    void createsAndReadsAgreementWithLegacyMasterFields() throws Exception {
        MvcResult typeResult = mockMvc.perform(post("/api/agreements/types")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "description": "Service Agreement Type",
                  "internalIdentifier": "service-agreement"
                }
                """))
            .andExpect(status().isCreated())
            .andReturn();
        String agreementTypeId = com.jayway.jsonpath.JsonPath.read(
            typeResult.getResponse().getContentAsString(),
            "$.id"
        );

        String payload = """
            {
              "tenantCode": "tenant-agreements",
              "agreementNumber": "agr-legacy-master-1",
              "name": "Legacy Master Agreement",
              "agreementType": "service",
              "effectiveFrom": "2026-03-01T00:00:00Z",
              "description": "  Legacy agreement description ",
              "agreementTypeId": "%s",
              "agreementStatus": " pending review ",
              "productId": 42,
              "agreementDate": "2026-02-01",
              "fromDate": "2026-03-01",
              "thruDate": "2027-03-01",
              "externalIdentifier": " EXT-AGR-1 ",
              "externalIdSource": " legacy "
            }
            """.formatted(agreementTypeId);

        mockMvc.perform(post("/api/agreements")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.description").value("Legacy agreement description"))
            .andExpect(jsonPath("$.agreementTypeId").value(agreementTypeId))
            .andExpect(jsonPath("$.agreementStatus").value("pending review"))
            .andExpect(jsonPath("$.productId").value(42))
            .andExpect(jsonPath("$.agreementDate").value("2026-02-01"))
            .andExpect(jsonPath("$.fromDate").value("2026-03-01"))
            .andExpect(jsonPath("$.thruDate").value("2027-03-01"))
            .andExpect(jsonPath("$.externalIdentifier").value("EXT-AGR-1"))
            .andExpect(jsonPath("$.externalIdSource").value("legacy"))
            .andExpect(jsonPath("$.status").value("DRAFT"));

        mockMvc.perform(AgreementsWebIntegrationTestSupport.getAgreementRequest("agr-legacy-master-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.agreementTypeId").value(agreementTypeId))
            .andExpect(jsonPath("$.agreementStatus").value("pending review"))
            .andExpect(jsonPath("$.productId").value(42))
            .andExpect(jsonPath("$.agreementDate").value("2026-02-01"))
            .andExpect(jsonPath("$.fromDate").value("2026-03-01"))
            .andExpect(jsonPath("$.thruDate").value("2027-03-01"))
            .andExpect(jsonPath("$.externalIdentifier").value("EXT-AGR-1"))
            .andExpect(jsonPath("$.externalIdSource").value("legacy"));
    }

    @Test
    void rejectsUnknownAgreementTypeReference() throws Exception {
        String agreementTypeId = "00000000-0000-0000-0000-000000000099";

        mockMvc.perform(post("/api/agreements")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "tenantCode": "tenant-agreements",
                  "agreementNumber": "agr-unknown-type",
                  "name": "Unknown Type Agreement",
                  "agreementType": "service",
                  "effectiveFrom": "2026-03-01T00:00:00Z",
                  "agreementTypeId": "%s"
                }
                """.formatted(agreementTypeId)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Agreement type not found for id: " + agreementTypeId));
    }

    @Test
    void returnsErrorEnvelopeForDuplicateAgreementNumber() throws Exception {
        String payload = """
            {
              "tenantCode": "tenant-agreements",
              "agreementNumber": "agr-3001",
              "name": "Master Services Agreement",
              "agreementType": "service",
              "effectiveFrom": "2026-03-01T00:00:00Z"
            }
            """;

        mockMvc.perform(post("/api/agreements")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/agreements")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("Agreement number already exists: AGR-3001"))
            .andExpect(jsonPath("$.path").value("/api/agreements"));
    }

    @Test
    void getsAgreementByAgreementNumber() throws Exception {
        String payload = """
            {
              "tenantCode": "tenant-agreements",
              "agreementNumber": "agr-3004",
              "name": "Read Agreement",
              "agreementType": "service",
              "effectiveFrom": "2026-03-01T00:00:00Z"
            }
            """;

        mockMvc.perform(post("/api/agreements")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
            .andExpect(status().isCreated());

        mockMvc.perform(AgreementsWebIntegrationTestSupport.getAgreementRequest("agr-3004"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tenantCode").value("TENANT-AGREEMENTS"))
            .andExpect(jsonPath("$.agreementNumber").value("AGR-3004"))
            .andExpect(jsonPath("$.name").value("Read Agreement"))
            .andExpect(jsonPath("$.agreementType").value("SERVICE"))
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.effectiveFrom").value("2026-03-01T00:00:00Z"));
    }

    @Test
    void returnsNotFoundWhenGettingUnknownAgreement() throws Exception {
        mockMvc.perform(AgreementsWebIntegrationTestSupport.getAgreementRequest("agr-missing-read"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value("Agreement not found: AGR-MISSING-READ"))
            .andExpect(jsonPath("$.path").value("/api/agreements/agr-missing-read"));
    }

    @Test
    void listsAgreementsWithOptionalStatusFilter() throws Exception {

        AgreementsIntegrationTestSupport.createAgreement(mockMvc, "agr-3010", "Draft Agreement");

        AgreementsWebIntegrationTestSupport.createAgreement(mockMvc, "tenant-agreements-alt", "agr-3013", "Other Tenant Agreement")
            .andExpect(status().isCreated());

        AgreementsIntegrationTestSupport.createAgreement(mockMvc, "agr-3011", "Active Agreement");

        registerActor(AGREEMENTS_TENANT_CODE, "legal@arcanaerp.com");
        AgreementsIntegrationTestSupport.transitionAgreementStatus(
            mockMvc,
            "agr-3011",
            AgreementStatus.ACTIVE,
            AGREEMENTS_TENANT_CODE,
            "Initial activation",
            "legal@arcanaerp.com"
        )
            .andExpect(status().isOk());

        AgreementsIntegrationTestSupport.createAgreement(mockMvc, "agr-3012", "Terminated Agreement");

        registerActor(AGREEMENTS_ALT_TENANT_CODE, "legal@arcanaerp.com");
        AgreementsIntegrationTestSupport.transitionAgreementStatus(
            mockMvc,
            "agr-3012",
            AgreementStatus.TERMINATED,
            AGREEMENTS_ALT_TENANT_CODE,
            "Mutual termination",
            "legal@arcanaerp.com"
        )
            .andExpect(status().isOk());

        mockMvc.perform(AgreementsWebIntegrationTestSupport.listAgreementsRequest(0, 100))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(100))
            .andExpect(jsonPath("$.totalItems", greaterThanOrEqualTo(3)))
            .andExpect(jsonPath("$.items[?(@.agreementNumber=='AGR-3010')].status", hasItem("DRAFT")))
            .andExpect(jsonPath("$.items[?(@.agreementNumber=='AGR-3011')].status", hasItem("ACTIVE")))
            .andExpect(jsonPath("$.items[?(@.agreementNumber=='AGR-3012')].status", hasItem("TERMINATED")));

        mockMvc.perform(AgreementsWebIntegrationTestSupport.listAgreementsRequest(0, 100, "tenant-agreements", null))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[?(@.agreementNumber=='AGR-3010')].tenantCode", hasItem("TENANT-AGREEMENTS")))
            .andExpect(jsonPath("$.items[?(@.agreementNumber=='AGR-3013')]").isEmpty());

        mockMvc.perform(AgreementsWebIntegrationTestSupport.listAgreementsRequest(0, 100, "tenant-agreements", "ACTIVE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[?(@.agreementNumber=='AGR-3011')].status", hasItem("ACTIVE")))
            .andExpect(jsonPath("$.items[?(@.agreementNumber=='AGR-3010')]").isEmpty());

        mockMvc.perform(AgreementsWebIntegrationTestSupport.listAgreementsRequest(0, 100, "ACTIVE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[?(@.agreementNumber=='AGR-3011')].status", hasItem("ACTIVE")));

        mockMvc.perform(AgreementsWebIntegrationTestSupport.listAgreementsRequest(0, 100, "TERMINATED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[?(@.agreementNumber=='AGR-3012')].status", hasItem("TERMINATED")));

        mockMvc.perform(AgreementsWebIntegrationTestSupport.listAgreementsRequest(0, 100, "DRAFT"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[?(@.agreementNumber=='AGR-3010')].status", hasItem("DRAFT")));
    }

    @Test
    void listsAgreementStatusHistory() throws Exception {
        AgreementsIntegrationTestSupport.createAgreement(mockMvc, "agr-3020", "History Agreement");

        registerActor(AGREEMENTS_TENANT_CODE, "legal@arcanaerp.com");
        AgreementsIntegrationTestSupport.transitionAgreementStatus(
            mockMvc,
            "agr-3020",
            AgreementStatus.ACTIVE,
            AGREEMENTS_TENANT_CODE,
            "Initial activation",
            "LEGAL@ARCANAERP.COM"
        )
            .andExpect(status().isOk());

        mockMvc.perform(AgreementsWebIntegrationTestSupport.statusHistoryRequestDefault("agr-3020"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(10))
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].agreementNumber").value("AGR-3020"))
            .andExpect(jsonPath("$.items[0].previousStatus").value("DRAFT"))
            .andExpect(jsonPath("$.items[0].currentStatus").value("ACTIVE"))
            .andExpect(jsonPath("$.items[0].tenantCode").value(AGREEMENTS_TENANT_CODE))
            .andExpect(jsonPath("$.items[0].reason").value("Initial activation"))
            .andExpect(jsonPath("$.items[0].changedBy").value("legal@arcanaerp.com"))
            .andExpect(jsonPath("$.items[0].changedAt").isNotEmpty());
    }

    @Test
    void statusHistoryIgnoresNoOpTransitions() throws Exception {
        AgreementsIntegrationTestSupport.createAgreement(mockMvc, "agr-3021", "No-op Agreement");

        registerActor(AGREEMENTS_TENANT_CODE, "legal@arcanaerp.com");
        AgreementsIntegrationTestSupport.transitionAgreementStatus(
            mockMvc,
            "agr-3021",
            AgreementStatus.ACTIVE,
            AGREEMENTS_TENANT_CODE,
            "Initial activation",
            "LEGAL@ARCANAERP.COM"
        )
            .andExpect(status().isOk());

        AgreementsIntegrationTestSupport.transitionAgreementStatus(
            mockMvc,
            "agr-3021",
            AgreementStatus.ACTIVE,
            AGREEMENTS_TENANT_CODE,
            "Initial activation",
            "LEGAL@ARCANAERP.COM"
        )
            .andExpect(status().isOk());

        mockMvc.perform(AgreementsWebIntegrationTestSupport.statusHistoryRequestDefault("agr-3021"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].previousStatus").value("DRAFT"))
            .andExpect(jsonPath("$.items[0].currentStatus").value("ACTIVE"))
            .andExpect(jsonPath("$.items[0].tenantCode").value(AGREEMENTS_TENANT_CODE))
            .andExpect(jsonPath("$.items[0].reason").value("Initial activation"))
            .andExpect(jsonPath("$.items[0].changedBy").value("legal@arcanaerp.com"));
    }

    @Test
    void filtersStatusHistoryByTenantAndChangedByAndChangedAtRange() throws Exception {
        AgreementsIntegrationTestSupport.createAgreement(mockMvc, "agr-3025", "Filtered History Agreement");

        registerActor(AGREEMENTS_TENANT_CODE, "legal@arcanaerp.com");
        AgreementsIntegrationTestSupport.transitionAgreementStatus(
            mockMvc,
            "agr-3025",
            AgreementStatus.ACTIVE,
            AGREEMENTS_TENANT_CODE,
            "Initial activation",
            "LEGAL@ARCANAERP.COM"
        )
            .andExpect(status().isOk());

        mockMvc.perform(AgreementsWebIntegrationTestSupport.statusHistoryRequestDefault(
            "agr-3025",
            "tenantCode",
            AGREEMENTS_TENANT_CODE
        ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].tenantCode").value(AGREEMENTS_TENANT_CODE));

        mockMvc.perform(AgreementsWebIntegrationTestSupport.statusHistoryRequestDefault(
            "agr-3025",
            "tenantCode",
            AGREEMENTS_ALT_TENANT_CODE
        ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(0));

        mockMvc.perform(AgreementsWebIntegrationTestSupport.statusHistoryRequestDefault(
            "agr-3025",
            "changedBy",
            "LEGAL@ARCANAERP.COM"
        ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].changedBy").value("legal@arcanaerp.com"));

        mockMvc.perform(AgreementsWebIntegrationTestSupport.statusHistoryRequestDefault(
            "agr-3025",
            "changedBy",
            "ops@arcanaerp.com"
        ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(0));

        mockMvc.perform(AgreementsWebIntegrationTestSupport.statusHistoryRequestDefault(
            "agr-3025",
            "changedAtFrom",
            "2000-01-01T00:00:00Z",
            "changedAtTo",
            "2100-01-01T00:00:00Z"
        ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1));

        mockMvc.perform(AgreementsWebIntegrationTestSupport.statusHistoryRequestDefault(
            "agr-3025",
            "changedAtFrom",
            "2100-01-01T00:00:00Z"
        ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(0));
    }

    @Test
    void rejectsStatusHistoryFilterWhenTenantCodeBlank() throws Exception {
        mockMvc.perform(AgreementsWebIntegrationTestSupport.statusHistoryRequestDefault(
            "agr-3025",
            "tenantCode",
            "   "
        ))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("tenantCode query parameter must not be blank"))
            .andExpect(jsonPath("$.path").value("/api/agreements/agr-3025/status-history"));
    }

    @Test
    void rejectsStatusHistoryFilterWhenChangedByBlank() throws Exception {
        mockMvc.perform(AgreementsWebIntegrationTestSupport.statusHistoryRequestDefault(
            "agr-3025",
            "changedBy",
            "   "
        ))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("changedBy query parameter must not be blank"))
            .andExpect(jsonPath("$.path").value("/api/agreements/agr-3025/status-history"));
    }

    @Test
    void rejectsStatusHistoryFilterWhenChangedAtFromInvalid() throws Exception {
        mockMvc.perform(AgreementsWebIntegrationTestSupport.statusHistoryRequestDefault(
            "agr-3025",
            "changedAtFrom",
            "not-a-timestamp"
        ))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("changedAtFrom query parameter must be a valid ISO-8601 instant"))
            .andExpect(jsonPath("$.path").value("/api/agreements/agr-3025/status-history"));
    }

    @Test
    void rejectsStatusHistoryFilterWhenChangedAtRangeInvalid() throws Exception {
        mockMvc.perform(AgreementsWebIntegrationTestSupport.statusHistoryRequestDefault(
            "agr-3025",
            "changedAtFrom",
            "2026-03-02T00:00:00Z",
            "changedAtTo",
            "2026-03-01T00:00:00Z"
        ))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("changedAtFrom must be before or equal to changedAtTo"))
            .andExpect(jsonPath("$.path").value("/api/agreements/agr-3025/status-history"));
    }

    @Test
    void statusHistoryReturnsNotFoundForUnknownAgreement() throws Exception {
        mockMvc.perform(AgreementsWebIntegrationTestSupport.statusHistoryRequestDefault("agr-missing-history"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value("Agreement not found: AGR-MISSING-HISTORY"))
            .andExpect(jsonPath("$.path").value("/api/agreements/agr-missing-history/status-history"));
    }

    @Test
    void rejectsInvalidStatusQueryFilter() throws Exception {
        mockMvc.perform(AgreementsWebIntegrationTestSupport.listAgreementsRequest(0, 10, "invalid"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("status query parameter must be one of: DRAFT, ACTIVE, TERMINATED"))
            .andExpect(jsonPath("$.path").value("/api/agreements"));

        mockMvc.perform(AgreementsWebIntegrationTestSupport.listAgreementsRequest(0, 10, ""))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("status query parameter must not be blank"))
            .andExpect(jsonPath("$.path").value("/api/agreements"));
    }

    @Test
    void rejectsBlankAgreementTenantFilter() throws Exception {
        mockMvc.perform(AgreementsWebIntegrationTestSupport.listAgreementsRequest(0, 10, "   ", null))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("tenantCode query parameter must not be blank"))
            .andExpect(jsonPath("$.path").value("/api/agreements"));
    }

    @Test
    void rejectsStatusTransitionWhenReasonBlank() throws Exception {
        AgreementsIntegrationTestSupport.createAgreement(mockMvc, "agr-3022", "Reason Validation Agreement");

        AgreementsIntegrationTestSupport.transitionAgreementStatus(
            mockMvc,
            "agr-3022",
            AgreementStatus.ACTIVE,
            AGREEMENTS_TENANT_CODE,
            "   ",
            "legal@arcanaerp.com"
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("reason: must not be blank"))
            .andExpect(jsonPath("$.path").value("/api/agreements/agr-3022/status"));
    }

    @Test
    void rejectsStatusTransitionWhenActorUnknownInTenant() throws Exception {
        AgreementsIntegrationTestSupport.createAgreement(mockMvc, "agr-3023", "Unknown Actor Agreement");

        AgreementsIntegrationTestSupport.transitionAgreementStatus(
            mockMvc,
            "agr-3023",
            AgreementStatus.ACTIVE,
            UNKNOWN_TENANT_CODE,
            "Initial activation",
            "unknown@arcanaerp.com"
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message")
                .value("Agreement status actor not found in tenant "
                    + UNKNOWN_TENANT_CODE
                    + ": unknown@arcanaerp.com"))
            .andExpect(jsonPath("$.path").value("/api/agreements/agr-3023/status"));
    }

    @Test
    void rejectsStatusTransitionWhenActorExistsInDifferentTenant() throws Exception {
        AgreementsIntegrationTestSupport.createAgreement(mockMvc, "agr-3024", "Mismatched Actor Agreement");

        registerActor(MISMATCH_ACTOR_TENANT_CODE, "tenant.actor@arcanaerp.com");

        AgreementsIntegrationTestSupport.transitionAgreementStatus(
            mockMvc,
            "agr-3024",
            AgreementStatus.ACTIVE,
            MISMATCH_REQUEST_TENANT_CODE,
            "Initial activation",
            "tenant.actor@arcanaerp.com"
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message")
                .value("Agreement status actor not found in tenant "
                    + MISMATCH_REQUEST_TENANT_CODE
                    + ": tenant.actor@arcanaerp.com"))
            .andExpect(jsonPath("$.path").value("/api/agreements/agr-3024/status"));
    }

    @Test
    void transitionsAgreementStatusFromDraftToActive() throws Exception {
        AgreementsIntegrationTestSupport.createAgreement(mockMvc, "agr-3002", "Master Services Agreement")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("DRAFT"));

        registerActor(AGREEMENTS_TENANT_CODE, "legal@arcanaerp.com");
        AgreementsIntegrationTestSupport.transitionAgreementStatus(
            mockMvc,
            "agr-3002",
            AgreementStatus.ACTIVE,
            AGREEMENTS_TENANT_CODE,
            "Initial activation",
            "legal@arcanaerp.com"
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.agreementNumber").value("AGR-3002"))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.activatedAt").isNotEmpty())
            .andExpect(jsonPath("$.terminatedAt").value(nullValue()));
    }

    @Test
    void transitionsAgreementStatusFromActiveToTerminated() throws Exception {
        AgreementsIntegrationTestSupport.createAgreement(mockMvc, "agr-3003", "Master Services Agreement");

        registerActor(AGREEMENTS_TENANT_CODE, "legal@arcanaerp.com");
        AgreementsIntegrationTestSupport.transitionAgreementStatus(
            mockMvc,
            "agr-3003",
            AgreementStatus.ACTIVE,
            AGREEMENTS_TENANT_CODE,
            "Initial activation",
            "legal@arcanaerp.com"
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACTIVE"));

        testClock.advance(Duration.ofSeconds(1));
        AgreementsIntegrationTestSupport.transitionAgreementStatus(
            mockMvc,
            "agr-3003",
            AgreementStatus.TERMINATED,
            AGREEMENTS_TENANT_CODE,
            "Termination attempt",
            "legal@arcanaerp.com"
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("TERMINATED"))
            .andExpect(jsonPath("$.activatedAt").isNotEmpty())
            .andExpect(jsonPath("$.terminatedAt").isNotEmpty());

        mockMvc.perform(AgreementsWebIntegrationTestSupport.statusHistoryRequestDefault("agr-3003"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.items[0].previousStatus").value("ACTIVE"))
            .andExpect(jsonPath("$.items[0].currentStatus").value("TERMINATED"))
            .andExpect(jsonPath("$.items[1].previousStatus").value("DRAFT"))
            .andExpect(jsonPath("$.items[1].currentStatus").value("ACTIVE"));
    }

    @Test
    void rejectsInvalidAgreementStatusTransitionFromTerminated() throws Exception {
        AgreementsIntegrationTestSupport.createAgreement(mockMvc, "agr-3006", "Master Services Agreement");

        registerActor(AGREEMENTS_TENANT_CODE, "legal@arcanaerp.com");
        AgreementsIntegrationTestSupport.transitionAgreementStatus(
            mockMvc,
            "agr-3006",
            AgreementStatus.ACTIVE,
            AGREEMENTS_TENANT_CODE,
            "Initial activation",
            "legal@arcanaerp.com"
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACTIVE"));

        AgreementsIntegrationTestSupport.transitionAgreementStatus(
            mockMvc,
            "agr-3006",
            AgreementStatus.TERMINATED,
            AGREEMENTS_TENANT_CODE,
            "Termination attempt",
            "legal@arcanaerp.com"
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("TERMINATED"));

        AgreementsIntegrationTestSupport.transitionAgreementStatus(
            mockMvc,
            "agr-3006",
            AgreementStatus.ACTIVE,
            AGREEMENTS_TENANT_CODE,
            "Initial activation",
            "legal@arcanaerp.com"
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("Agreement status transition not allowed: TERMINATED -> ACTIVE"))
            .andExpect(jsonPath("$.path").value("/api/agreements/agr-3006/status"));
    }

    @Test
    void returnsNotFoundWhenChangingStatusForUnknownAgreement() throws Exception {
        AgreementsIntegrationTestSupport.transitionAgreementStatus(
            mockMvc,
            "agr-missing",
            AgreementStatus.ACTIVE,
            AGREEMENTS_TENANT_CODE,
            "Initial activation",
            "legal@arcanaerp.com"
        )
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value("Agreement not found: AGR-MISSING"))
            .andExpect(jsonPath("$.path").value("/api/agreements/agr-missing/status"));
    }

    private String createAgreementRoleType(String internalIdentifier, String description) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/agreements/role-types")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "description": "%s",
                  "internalIdentifier": "%s"
                }
                """.formatted(description, internalIdentifier)))
            .andExpect(status().isCreated())
            .andReturn();
        return com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private String createAgreementAndReturnId(String tenantCode, String agreementNumber, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/agreements")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "tenantCode": "%s",
                  "agreementNumber": "%s",
                  "name": "%s",
                  "agreementType": "service",
                  "effectiveFrom": "2026-03-01T00:00:00Z"
                }
                """.formatted(tenantCode, agreementNumber, name)))
            .andExpect(status().isCreated())
            .andReturn();
        return com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private void registerActor(String tenantCode, String email) throws Exception {
        AgreementsIntegrationTestSupport.registerActor(
            mockMvc,
            tenantCode,
            email,
            "Agreements Tenant",
            "Agreements Actor"
        );
    }
}
