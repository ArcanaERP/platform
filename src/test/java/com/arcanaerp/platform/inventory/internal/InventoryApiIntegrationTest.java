package com.arcanaerp.platform.inventory.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.arcanaerp.platform.core.api.ConflictException;
import com.arcanaerp.platform.core.uom.RegisterUnitOfMeasurementCommand;
import com.arcanaerp.platform.core.uom.UnitOfMeasurementDirectory;
import com.arcanaerp.platform.identity.RegisterUserCommand;
import com.arcanaerp.platform.identity.UserDirectory;
import com.arcanaerp.platform.identity.UserView;
import com.arcanaerp.platform.testsupport.web.InventoryManagementWebTestSupport;
import com.arcanaerp.platform.testsupport.web.InventoryTransferReversalHistoryWebTestSupport;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class InventoryApiIntegrationTest {

    private static final UUID PENDING_REVERSAL_TRANSFER_ID = new UUID(0L, 0L);
    private static final String DEFAULT_TRANSFER_REASON = InventoryReversalTestConstants.TRANSFER_REASON;
    private static final String DEFAULT_ACTOR = InventoryReversalTestConstants.REVERSAL_ACTOR;
    private static final String DEFAULT_REVERSAL_REASON = InventoryReversalTestConstants.REVERSAL_REASON;
    private static final Instant SEED_INSTANT = Instant.parse("2026-03-01T00:00:00Z");
    private static final Instant STALE_PENDING_CLAIM_AT = Instant.parse("2025-12-01T00:00:00Z");
    private static final BigDecimal DEFAULT_MAIN_ON_HAND = new BigDecimal("10");
    private static final BigDecimal DEFAULT_EAST_ON_HAND = new BigDecimal("4");
    private static final BigDecimal TRANSFER_BY_ID_MAIN_ON_HAND = new BigDecimal("11");
    private static final BigDecimal TRANSFER_BY_ID_EAST_ON_HAND = new BigDecimal("2");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private InventoryItemMetadataChangeAuditRepository metadataChangeAuditRepository;

    @Autowired
    private InventoryItemOwnerChangeAuditRepository ownerChangeAuditRepository;

    @Autowired
    private InventoryItemAvailabilityChangeAuditRepository availabilityChangeAuditRepository;

    @Autowired
    private InventoryAdjustmentRepository inventoryAdjustmentRepository;

    @Autowired
    private InventoryPickupDropoffTransactionRepository pickupDropoffTransactionRepository;

    @Autowired
    private EntityManager entityManager;

    @MockitoSpyBean
    private InventoryTransferReversalIdempotencyRepository reversalIdempotencyRepository;

    @Autowired
    private InventoryLocationRepository inventoryLocationRepository;

    @Autowired
    private InventoryFacilityRepository inventoryFacilityRepository;

    @Autowired
    private InventoryFacilityActiveChangeAuditRepository facilityActiveChangeAuditRepository;

    @Autowired
    private InventoryFacilityMetadataChangeAuditRepository facilityMetadataChangeAuditRepository;

    @Autowired
    private InventoryFacilityPartyRoleAssignmentRepository facilityPartyRoleAssignmentRepository;

    @Autowired
    private InventoryFacilityPartyRoleAssignmentEndAuditRepository facilityPartyRoleAssignmentEndAuditRepository;

    @Autowired
    private InventoryFixedAssetRepository inventoryFixedAssetRepository;

    @Autowired
    private InventoryFixedAssetTypeRepository inventoryFixedAssetTypeRepository;

    @Autowired
    private InventoryFixedAssetActiveChangeAuditRepository fixedAssetActiveChangeAuditRepository;

    @Autowired
    private InventoryFixedAssetMetadataChangeAuditRepository fixedAssetMetadataChangeAuditRepository;

    @Autowired
    private InventoryFixedAssetPartyRoleAssignmentRepository fixedAssetPartyRoleAssignmentRepository;

    @Autowired
    private InventoryFixedAssetPartyRoleAssignmentEndAuditRepository fixedAssetPartyRoleAssignmentEndAuditRepository;

    @Autowired
    private InventoryPartyRepository inventoryPartyRepository;

    @Autowired
    private InventoryPartyRoleTypeRepository inventoryPartyRoleTypeRepository;

    @Autowired
    private InventoryLocationMetadataChangeAuditRepository locationMetadataChangeAuditRepository;

    @Autowired
    private InventoryLocationTypeRepository inventoryLocationTypeRepository;

    @Autowired
    private InventoryEntryRelationshipTypeRepository inventoryEntryRelationshipTypeRepository;

    @Autowired
    private InventoryEntryRoleTypeRepository inventoryEntryRoleTypeRepository;

    @Autowired
    private InventoryEntryRelationshipRepository inventoryEntryRelationshipRepository;

    @Autowired
    private InventoryEntryRelationshipStatusChangeAuditRepository inventoryEntryRelationshipStatusChangeAuditRepository;

    @Autowired
    private InventoryProductInstanceAssignmentRepository inventoryProductInstanceAssignmentRepository;

    @Autowired
    private InventoryProductInstanceAssignmentReleaseAuditRepository inventoryProductInstanceAssignmentReleaseAuditRepository;

    @Autowired
    private InventoryItemLocationAssignmentRepository inventoryItemLocationAssignmentRepository;

    @Autowired
    private InventoryItemLocationAssignmentEndAuditRepository inventoryItemLocationAssignmentEndAuditRepository;

    @Autowired
    private UnitOfMeasurementDirectory unitOfMeasurementDirectory;

    @Autowired
    private UserDirectory userDirectory;

    @BeforeEach
    void cleanInventoryItems() {
        reset(reversalIdempotencyRepository);
        reversalIdempotencyRepository.deleteAll();
        inventoryEntryRelationshipStatusChangeAuditRepository.deleteAll();
        inventoryEntryRelationshipRepository.deleteAll();
        inventoryProductInstanceAssignmentReleaseAuditRepository.deleteAll();
        inventoryProductInstanceAssignmentRepository.deleteAll();
        inventoryItemLocationAssignmentEndAuditRepository.deleteAll();
        inventoryItemLocationAssignmentRepository.deleteAll();
        availabilityChangeAuditRepository.deleteAll();
        ownerChangeAuditRepository.deleteAll();
        metadataChangeAuditRepository.deleteAll();
        pickupDropoffTransactionRepository.deleteAll();
        inventoryAdjustmentRepository.deleteAll();
        inventoryItemRepository.deleteAll();
        fixedAssetActiveChangeAuditRepository.deleteAll();
        fixedAssetMetadataChangeAuditRepository.deleteAll();
        fixedAssetPartyRoleAssignmentEndAuditRepository.deleteAll();
        fixedAssetPartyRoleAssignmentRepository.deleteAll();
        inventoryFixedAssetRepository.deleteAll();
        inventoryFixedAssetTypeRepository.deleteAll();
        facilityActiveChangeAuditRepository.deleteAll();
        facilityMetadataChangeAuditRepository.deleteAll();
        facilityPartyRoleAssignmentEndAuditRepository.deleteAll();
        facilityPartyRoleAssignmentRepository.deleteAll();
        inventoryFacilityRepository.deleteAll();
        inventoryPartyRoleTypeRepository.deleteAll();
        inventoryPartyRepository.deleteAll();
        locationMetadataChangeAuditRepository.deleteAll();
        inventoryLocationRepository.deleteAll();
        inventoryLocationTypeRepository.deleteAll();
        inventoryEntryRelationshipTypeRepository.deleteAll();
        inventoryEntryRoleTypeRepository.deleteAll();
        seedLocationType("WAREHOUSE", "Warehouse");
        seedLocationType("STORE", "Store");
        seedFixedAssetType("VEHICLE", "Vehicle");
        seedInventoryParty("WEST-OPERATOR", "West operator");
        seedInventoryParty("WEST-CARRIER", "West carrier");
        seedInventoryPartyRoleType("MANAGER", "Manager");
        seedInventoryPartyRoleType("OPERATOR", "Operator");
        ensureUnitOfMeasurement("EA", "Each");
        ensureUnitOfMeasurement("CASE", "Case");
        ensureUnitOfMeasurement("EACH", "Each alternate");
        ensureUnitOfMeasurement("PALLET", "Pallet");
    }

    @Test
    void returnsInventoryBySkuUsingDefaultMainLocation() throws Exception {
        inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9200",
                "main",
                new BigDecimal("25"),
                SEED_INSTANT
            )
        );

        mockMvc.perform(get("/api/inventory/{sku}", "arc-9200"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sku").value("ARC-9200"))
            .andExpect(jsonPath("$.locationCode").value("MAIN"))
            .andExpect(jsonPath("$.onHandQuantity").value(25))
            .andExpect(jsonPath("$.unitOfMeasurementCode").value("EA"))
            .andExpect(jsonPath("$.classificationCode").value("ON_HAND"))
            .andExpect(jsonPath("$.updatedAt").value("2026-03-01T00:00:00Z"));
    }

    @Test
    void returnsInventoryBySkuAtSpecificLocation() throws Exception {
        inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9201",
                "wh-west",
                new BigDecimal("7"),
                SEED_INSTANT
            )
        );

        mockMvc.perform(get("/api/inventory/{sku}", "arc-9201")
            .param("locationCode", "wh-west"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sku").value("ARC-9201"))
            .andExpect(jsonPath("$.locationCode").value("WH-WEST"))
            .andExpect(jsonPath("$.onHandQuantity").value(7));
    }

    @Test
    void returnsInventoryItemClassificationAndUnitOfMeasurementMetadata() throws Exception {
        inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9201a",
                "wh-west",
                new BigDecimal("12"),
                "case",
                "quarantine",
                SEED_INSTANT
            )
        );

        mockMvc.perform(get("/api/inventory/{sku}", "arc-9201a")
            .param("locationCode", "wh-west"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sku").value("ARC-9201A"))
            .andExpect(jsonPath("$.locationCode").value("WH-WEST"))
            .andExpect(jsonPath("$.onHandQuantity").value(12))
            .andExpect(jsonPath("$.unitOfMeasurementCode").value("CASE"))
            .andExpect(jsonPath("$.classificationCode").value("QUARANTINE"));
    }

    @Test
    void returnsNotFoundForUnknownSkuAtDefaultMainLocation() throws Exception {
        expectInventoryItemNotFound(
            mockMvc.perform(get("/api/inventory/{sku}", "arc-9202")),
            "arc-9202",
            "main",
            "/api/inventory/arc-9202"
        );
    }

    @Test
    void rejectsBlankLocationCodeQueryParam() throws Exception {
        expectBadRequest(
            mockMvc.perform(get("/api/inventory/{sku}", "arc-9202")
                .param("locationCode", "   ")),
            "locationCode query parameter must not be blank",
            "/api/inventory/arc-9202"
        );
    }

    @Test
    void createsReadsAndListsInventoryLocationTypes() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/inventory/location-types")
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {
                  "code": " cross_dock ",
                  "description": " Cross-dock facility "
                }
                """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.code").value("CROSS_DOCK"))
            .andExpect(jsonPath("$.description").value("Cross-dock facility"))
            .andExpect(jsonPath("$.createdAt").isNotEmpty());

        mockMvc.perform(get("/api/inventory/location-types/{code}", "cross_dock"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("CROSS_DOCK"))
            .andExpect(jsonPath("$.description").value("Cross-dock facility"));

        mockMvc.perform(get("/api/inventory/location-types")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(3))
            .andExpect(jsonPath("$.items[0].code").value("CROSS_DOCK"))
            .andExpect(jsonPath("$.items[1].code").value("STORE"))
            .andExpect(jsonPath("$.items[2].code").value("WAREHOUSE"));
    }

    @Test
    void rejectsDuplicateInventoryLocationTypeCode() throws Exception {
        String payload = """
            {
              "code": "cross_dock",
              "description": "Cross-dock facility"
            }
            """;
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/inventory/location-types")
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content(payload))
            .andExpect(status().isCreated());

        expectConflict(
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/inventory/location-types")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(payload)),
            "Inventory location type already exists for code: CROSS_DOCK",
            "/api/inventory/location-types"
        );
    }

    @Test
    void createsReadsAndListsInventoryFixedAssetTypes() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/inventory/fixed-asset-types")
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {
                  "code": " forklift ",
                  "description": " Forklift "
                }
                """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.code").value("FORKLIFT"))
            .andExpect(jsonPath("$.description").value("Forklift"))
            .andExpect(jsonPath("$.createdAt").isNotEmpty());

        mockMvc.perform(get("/api/inventory/fixed-asset-types/{code}", "forklift"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("FORKLIFT"))
            .andExpect(jsonPath("$.description").value("Forklift"));

        mockMvc.perform(get("/api/inventory/fixed-asset-types")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.items[0].code").value("FORKLIFT"))
            .andExpect(jsonPath("$.items[1].code").value("VEHICLE"));
    }

    @Test
    void rejectsDuplicateInventoryFixedAssetTypeCode() throws Exception {
        String payload = """
            {
              "code": "forklift",
              "description": "Forklift"
            }
            """;
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/inventory/fixed-asset-types")
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content(payload))
            .andExpect(status().isCreated());

        expectConflict(
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/inventory/fixed-asset-types")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(payload)),
            "Inventory fixed asset type already exists for code: FORKLIFT",
            "/api/inventory/fixed-asset-types"
        );
    }

    @Test
    void createsReadsAndListsInventoryParties() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/inventory/parties")
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {
                  "code": " service-vendor ",
                  "description": " Service vendor "
                }
                """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.code").value("SERVICE-VENDOR"))
            .andExpect(jsonPath("$.description").value("Service vendor"))
            .andExpect(jsonPath("$.createdAt").isNotEmpty());

        mockMvc.perform(get("/api/inventory/parties/{code}", "service-vendor"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("SERVICE-VENDOR"))
            .andExpect(jsonPath("$.description").value("Service vendor"));

        mockMvc.perform(get("/api/inventory/parties")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(3))
            .andExpect(jsonPath("$.items[0].code").value("SERVICE-VENDOR"))
            .andExpect(jsonPath("$.items[1].code").value("WEST-CARRIER"))
            .andExpect(jsonPath("$.items[2].code").value("WEST-OPERATOR"));
    }

    @Test
    void rejectsDuplicateInventoryPartyCode() throws Exception {
        String payload = """
            {
              "code": "service-vendor",
              "description": "Service vendor"
            }
            """;
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/inventory/parties")
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content(payload))
            .andExpect(status().isCreated());

        expectConflict(
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/inventory/parties")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(payload)),
            "Inventory party already exists for code: SERVICE-VENDOR",
            "/api/inventory/parties"
        );
    }

    @Test
    void createsReadsAndListsInventoryPartyRoleTypes() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/inventory/party-role-types")
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {
                  "code": " maintainer ",
                  "description": " Maintainer "
                }
                """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.code").value("MAINTAINER"))
            .andExpect(jsonPath("$.description").value("Maintainer"))
            .andExpect(jsonPath("$.createdAt").isNotEmpty());

        mockMvc.perform(get("/api/inventory/party-role-types/{code}", "maintainer"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("MAINTAINER"))
            .andExpect(jsonPath("$.description").value("Maintainer"));

        mockMvc.perform(get("/api/inventory/party-role-types")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(3))
            .andExpect(jsonPath("$.items[0].code").value("MAINTAINER"))
            .andExpect(jsonPath("$.items[1].code").value("MANAGER"))
            .andExpect(jsonPath("$.items[2].code").value("OPERATOR"));
    }

    @Test
    void rejectsDuplicateInventoryPartyRoleTypeCode() throws Exception {
        String payload = """
            {
              "code": "maintainer",
              "description": "Maintainer"
            }
            """;
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/inventory/party-role-types")
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content(payload))
            .andExpect(status().isCreated());

        expectConflict(
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/inventory/party-role-types")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(payload)),
            "Inventory party role type already exists for code: MAINTAINER",
            "/api/inventory/party-role-types"
        );
    }

    @Test
    void createsReadsAndListsInventoryEntryRelationshipTypes() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
            "/api/inventory/entry-relationship-types"
        )
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {
                  "code": " component_of ",
                  "description": " Component of kit ",
                  "comments": " Used for kit composition "
                }
                """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.code").value("COMPONENT_OF"))
            .andExpect(jsonPath("$.description").value("Component of kit"))
            .andExpect(jsonPath("$.comments").value("Used for kit composition"))
            .andExpect(jsonPath("$.createdAt").isNotEmpty());

        mockMvc.perform(get("/api/inventory/entry-relationship-types/{code}", "component_of"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("COMPONENT_OF"))
            .andExpect(jsonPath("$.description").value("Component of kit"));

        mockMvc.perform(get("/api/inventory/entry-relationship-types")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].code").value("COMPONENT_OF"));
    }

    @Test
    void rejectsDuplicateInventoryEntryRelationshipTypeCode() throws Exception {
        String payload = """
            {
              "code": "component_of",
              "description": "Component of kit"
            }
            """;
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
            "/api/inventory/entry-relationship-types"
        )
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content(payload))
            .andExpect(status().isCreated());

        expectConflict(
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                "/api/inventory/entry-relationship-types"
            )
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(payload)),
            "Inventory entry relationship type already exists for code: COMPONENT_OF",
            "/api/inventory/entry-relationship-types"
        );
    }

    @Test
    void createsReadsAndListsInventoryEntryRoleTypes() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
            "/api/inventory/entry-role-types"
        )
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {
                  "code": " source_entry ",
                  "description": " Source inventory entry ",
                  "comments": " Relationship origin role "
                }
                """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.code").value("SOURCE_ENTRY"))
            .andExpect(jsonPath("$.description").value("Source inventory entry"))
            .andExpect(jsonPath("$.comments").value("Relationship origin role"))
            .andExpect(jsonPath("$.createdAt").isNotEmpty());

        mockMvc.perform(get("/api/inventory/entry-role-types/{code}", "source_entry"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("SOURCE_ENTRY"))
            .andExpect(jsonPath("$.description").value("Source inventory entry"));

        mockMvc.perform(get("/api/inventory/entry-role-types")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].code").value("SOURCE_ENTRY"));
    }

    @Test
    void rejectsDuplicateInventoryEntryRoleTypeCode() throws Exception {
        String payload = """
            {
              "code": "source_entry",
              "description": "Source inventory entry"
            }
            """;
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
            "/api/inventory/entry-role-types"
        )
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content(payload))
            .andExpect(status().isCreated());

        expectConflict(
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                "/api/inventory/entry-role-types"
            )
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(payload)),
            "Inventory entry role type already exists for code: SOURCE_ENTRY",
            "/api/inventory/entry-role-types"
        );
    }

    @Test
    void createsReadsAndListsInventoryEntryRelationships() throws Exception {
        seedInventoryEntryRelationshipReferenceData();
        inventoryItemRepository.save(InventoryItem.create(
            "arc-kit-100",
            "wh-kit",
            new BigDecimal("3"),
            SEED_INSTANT
        ));
        inventoryItemRepository.save(InventoryItem.create(
            "arc-component-100",
            "wh-kit",
            new BigDecimal("12"),
            SEED_INSTANT
        ));

        String relationshipId = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
            "/api/inventory/entry-relationships"
        )
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {
                  "relationshipTypeCode": " component_of ",
                  "fromSku": " arc-component-100 ",
                  "fromLocationCode": " wh-kit ",
                  "toSku": " arc-kit-100 ",
                  "toLocationCode": " wh-kit ",
                  "fromRoleTypeCode": " component ",
                  "toRoleTypeCode": " assembly ",
                  "description": " Component participates in kit ",
                  "statusCode": " active ",
                  "fromDate": "2026-03-02T00:00:00Z",
                  "thruDate": "2026-03-31T00:00:00Z"
                }
                """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.relationshipTypeCode").value("COMPONENT_OF"))
            .andExpect(jsonPath("$.fromSku").value("ARC-COMPONENT-100"))
            .andExpect(jsonPath("$.fromLocationCode").value("WH-KIT"))
            .andExpect(jsonPath("$.toSku").value("ARC-KIT-100"))
            .andExpect(jsonPath("$.toLocationCode").value("WH-KIT"))
            .andExpect(jsonPath("$.fromRoleTypeCode").value("COMPONENT"))
            .andExpect(jsonPath("$.toRoleTypeCode").value("ASSEMBLY"))
            .andExpect(jsonPath("$.description").value("Component participates in kit"))
            .andExpect(jsonPath("$.statusCode").value("ACTIVE"))
            .andExpect(jsonPath("$.fromDate").value("2026-03-02T00:00:00Z"))
            .andExpect(jsonPath("$.thruDate").value("2026-03-31T00:00:00Z"))
            .andExpect(jsonPath("$.createdAt").isNotEmpty())
            .andReturn()
            .getResponse()
            .getContentAsString()
            .replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(get("/api/inventory/entry-relationships/{id}", relationshipId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.relationshipTypeCode").value("COMPONENT_OF"))
            .andExpect(jsonPath("$.fromSku").value("ARC-COMPONENT-100"))
            .andExpect(jsonPath("$.toSku").value("ARC-KIT-100"))
            .andExpect(jsonPath("$.fromDate").value("2026-03-02T00:00:00Z"))
            .andExpect(jsonPath("$.thruDate").value("2026-03-31T00:00:00Z"));

        mockMvc.perform(get("/api/inventory/entry-relationships")
            .param("relationshipTypeCode", "component_of")
            .param("fromSku", "arc-component-100")
            .param("statusCode", "active")
            .param("fromDateFrom", "2026-03-01T00:00:00Z")
            .param("fromDateTo", "2026-03-03T00:00:00Z")
            .param("thruDateFrom", "2026-03-30T00:00:00Z")
            .param("thruDateTo", "2026-04-01T00:00:00Z")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].relationshipTypeCode").value("COMPONENT_OF"))
            .andExpect(jsonPath("$.items[0].fromSku").value("ARC-COMPONENT-100"));
    }

    @Test
    void updatesInventoryEntryRelationshipStatusAndListsHistory() throws Exception {
        String relationshipId = registerComponentRelationship(
            "arc-kit-110",
            "arc-component-110",
            "wh-kit"
        );

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
            "/api/inventory/entry-relationships/{id}/status",
            relationshipId
        )
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {
                  "statusCode": " inactive ",
                  "reason": " Component relationship retired ",
                  "changedBy": " Inventory.Manager "
                }
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(relationshipId))
            .andExpect(jsonPath("$.statusCode").value("INACTIVE"))
            .andExpect(jsonPath("$.updatedAt").isNotEmpty());

        mockMvc.perform(get("/api/inventory/entry-relationships/{id}/status-history", relationshipId)
            .param("changedBy", "inventory.manager")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].relationshipId").value(relationshipId))
            .andExpect(jsonPath("$.items[0].previousStatusCode").value("ACTIVE"))
            .andExpect(jsonPath("$.items[0].currentStatusCode").value("INACTIVE"))
            .andExpect(jsonPath("$.items[0].reason").value("Component relationship retired"))
            .andExpect(jsonPath("$.items[0].changedBy").value("inventory.manager"))
            .andExpect(jsonPath("$.items[0].changedAt").isNotEmpty());
    }

    @Test
    void rejectsNoOpInventoryEntryRelationshipStatusUpdate() throws Exception {
        String relationshipId = registerComponentRelationship(
            "arc-kit-111",
            "arc-component-111",
            "wh-kit"
        );

        expectBadRequest(
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
                "/api/inventory/entry-relationships/{id}/status",
                relationshipId
            )
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "statusCode": " active ",
                      "reason": " No change ",
                      "changedBy": "inventory.manager"
                    }
                    """)),
            "Inventory entry relationship status is already ACTIVE",
            "/api/inventory/entry-relationships/" + relationshipId + "/status"
        );
    }

    @Test
    void rejectsInventoryEntryRelationshipWithUnknownRelationshipType() throws Exception {
        seedInventoryEntryRoleType("component", "Component");
        seedInventoryEntryRoleType("assembly", "Assembly");
        inventoryItemRepository.save(InventoryItem.create("arc-kit-101", "wh-kit", new BigDecimal("3"), SEED_INSTANT));
        inventoryItemRepository.save(InventoryItem.create("arc-component-101", "wh-kit", new BigDecimal("12"), SEED_INSTANT));

        expectBadRequest(
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                "/api/inventory/entry-relationships"
            )
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "relationshipTypeCode": "missing_type",
                      "fromSku": "arc-component-101",
                      "fromLocationCode": "wh-kit",
                      "toSku": "arc-kit-101",
                      "toLocationCode": "wh-kit",
                      "fromRoleTypeCode": "component",
                      "toRoleTypeCode": "assembly",
                      "description": "Missing type"
                    }
                    """)),
            "Inventory entry relationship type not found: MISSING_TYPE",
            "/api/inventory/entry-relationships"
        );
    }

    @Test
    void rejectsInventoryEntryRelationshipToSameItem() throws Exception {
        seedInventoryEntryRelationshipReferenceData();
        inventoryItemRepository.save(InventoryItem.create("arc-kit-102", "wh-kit", new BigDecimal("3"), SEED_INSTANT));

        expectBadRequest(
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                "/api/inventory/entry-relationships"
            )
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "relationshipTypeCode": "component_of",
                      "fromSku": "arc-kit-102",
                      "fromLocationCode": "wh-kit",
                      "toSku": "arc-kit-102",
                      "toLocationCode": "wh-kit",
                      "fromRoleTypeCode": "component",
                      "toRoleTypeCode": "assembly",
                      "description": "Self link"
                    }
                    """)),
            "from and to inventory items must be different",
            "/api/inventory/entry-relationships"
        );
    }

    @Test
    void rejectsInventoryEntryRelationshipWithInvalidDateWindow() throws Exception {
        seedInventoryEntryRelationshipReferenceData();
        inventoryItemRepository.save(InventoryItem.create("arc-kit-103", "wh-kit", new BigDecimal("3"), SEED_INSTANT));
        inventoryItemRepository.save(InventoryItem.create(
            "arc-component-103",
            "wh-kit",
            new BigDecimal("12"),
            SEED_INSTANT
        ));

        expectBadRequest(
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                "/api/inventory/entry-relationships"
            )
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "relationshipTypeCode": "component_of",
                      "fromSku": "arc-component-103",
                      "fromLocationCode": "wh-kit",
                      "toSku": "arc-kit-103",
                      "toLocationCode": "wh-kit",
                      "fromRoleTypeCode": "component",
                      "toRoleTypeCode": "assembly",
                      "description": "Invalid window",
                      "fromDate": "2026-03-31T00:00:00Z",
                      "thruDate": "2026-03-02T00:00:00Z"
                    }
                    """)),
            "thruDate must be after or equal to fromDate",
            "/api/inventory/entry-relationships"
        );
    }

    @Test
    void createsReadsAndListsInventoryProductInstanceAssignments() throws Exception {
        inventoryItemRepository.save(InventoryItem.create(
            "arc-serialized-100",
            "wh-serial",
            new BigDecimal("2"),
            SEED_INSTANT
        ));

        String assignmentId = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
            "/api/inventory/product-instance-assignments"
        )
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {
                  "sku": " arc-serialized-100 ",
                  "locationCode": " wh-serial ",
                  "productInstanceCode": " pi-serial-100 ",
                  "assignedBy": " Inventory.Manager "
                }
                """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.inventoryItemId").isNotEmpty())
            .andExpect(jsonPath("$.sku").value("ARC-SERIALIZED-100"))
            .andExpect(jsonPath("$.locationCode").value("WH-SERIAL"))
            .andExpect(jsonPath("$.productInstanceCode").value("PI-SERIAL-100"))
            .andExpect(jsonPath("$.assignedBy").value("inventory.manager"))
            .andExpect(jsonPath("$.assignedAt").isNotEmpty())
            .andExpect(jsonPath("$.active").value(true))
            .andExpect(jsonPath("$.releaseReason").doesNotExist())
            .andExpect(jsonPath("$.releasedBy").doesNotExist())
            .andExpect(jsonPath("$.releasedAt").doesNotExist())
            .andReturn()
            .getResponse()
            .getContentAsString()
            .replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(get("/api/inventory/product-instance-assignments/{id}", assignmentId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(assignmentId))
            .andExpect(jsonPath("$.productInstanceCode").value("PI-SERIAL-100"));

        mockMvc.perform(get("/api/inventory/product-instance-assignments")
            .param("sku", "arc-serialized-100")
            .param("locationCode", "wh-serial")
            .param("productInstanceCode", "pi-serial-100")
            .param("assignedBy", "inventory.manager")
            .param("active", "true")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].id").value(assignmentId))
            .andExpect(jsonPath("$.items[0].sku").value("ARC-SERIALIZED-100"));
    }

    @Test
    void releasesInventoryProductInstanceAssignmentAndListsReleaseHistory() throws Exception {
        String assignmentId = registerProductInstanceAssignment(
            "arc-serialized-110",
            "wh-serial",
            "pi-serial-110",
            "inventory.manager"
        );

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
            "/api/inventory/product-instance-assignments/{id}/release",
            assignmentId
        )
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {
                  "reason": " Unit consumed ",
                  "releasedBy": " Inventory.Manager "
                }
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(assignmentId))
            .andExpect(jsonPath("$.active").value(false))
            .andExpect(jsonPath("$.releaseReason").value("Unit consumed"))
            .andExpect(jsonPath("$.releasedBy").value("inventory.manager"))
            .andExpect(jsonPath("$.releasedAt").isNotEmpty());

        mockMvc.perform(get("/api/inventory/product-instance-assignments/{id}/release-history", assignmentId)
            .param("releasedBy", "inventory.manager")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].assignmentId").value(assignmentId))
            .andExpect(jsonPath("$.items[0].sku").value("ARC-SERIALIZED-110"))
            .andExpect(jsonPath("$.items[0].locationCode").value("WH-SERIAL"))
            .andExpect(jsonPath("$.items[0].productInstanceCode").value("PI-SERIAL-110"))
            .andExpect(jsonPath("$.items[0].reason").value("Unit consumed"))
            .andExpect(jsonPath("$.items[0].releasedBy").value("inventory.manager"))
            .andExpect(jsonPath("$.items[0].releasedAt").isNotEmpty());

        mockMvc.perform(get("/api/inventory/product-instance-assignments")
            .param("productInstanceCode", "pi-serial-110")
            .param("active", "false")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].id").value(assignmentId))
            .andExpect(jsonPath("$.items[0].active").value(false));
    }

    @Test
    void rejectsReleaseOfAlreadyReleasedInventoryProductInstanceAssignment() throws Exception {
        String assignmentId = registerProductInstanceAssignment(
            "arc-serialized-111",
            "wh-serial",
            "pi-serial-111",
            "inventory.manager"
        );
        String payload = """
            {
              "reason": "Unit consumed",
              "releasedBy": "inventory.manager"
            }
            """;

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
            "/api/inventory/product-instance-assignments/{id}/release",
            assignmentId
        )
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content(payload))
            .andExpect(status().isOk());

        expectBadRequest(
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
                "/api/inventory/product-instance-assignments/{id}/release",
                assignmentId
            )
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(payload)),
            "Inventory product instance assignment is already released",
            "/api/inventory/product-instance-assignments/" + assignmentId + "/release"
        );
    }

    @Test
    void rejectsDuplicateInventoryProductInstanceAssignment() throws Exception {
        inventoryItemRepository.save(InventoryItem.create(
            "arc-serialized-101",
            "wh-serial",
            new BigDecimal("2"),
            SEED_INSTANT
        ));
        String payload = """
            {
              "sku": "arc-serialized-101",
              "locationCode": "wh-serial",
              "productInstanceCode": "pi-serial-101",
              "assignedBy": "inventory.manager"
            }
            """;

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
            "/api/inventory/product-instance-assignments"
        )
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content(payload))
            .andExpect(status().isCreated());

        expectConflict(
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                "/api/inventory/product-instance-assignments"
            )
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(payload)),
            "Inventory product instance assignment already exists for SKU: "
                + "ARC-SERIALIZED-101 at location: WH-SERIAL and product instance: PI-SERIAL-101",
            "/api/inventory/product-instance-assignments"
        );
    }

    @Test
    void rejectsInventoryProductInstanceAssignmentForUnknownItem() throws Exception {
        expectInventoryItemNotFound(
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                "/api/inventory/product-instance-assignments"
            )
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "sku": "missing-sku",
                      "locationCode": "missing-location",
                      "productInstanceCode": "pi-missing",
                      "assignedBy": "inventory.manager"
                    }
                    """)),
            "missing-sku",
            "missing-location",
            "/api/inventory/product-instance-assignments"
        );
    }

    @Test
    void createsReadsAndListsInventoryItemLocationAssignments() throws Exception {
        inventoryItemRepository.save(InventoryItem.create(
            "arc-location-100",
            "wh-source",
            new BigDecimal("2"),
            SEED_INSTANT
        ));
        inventoryLocationRepository.save(InventoryLocation.create("bin-a", "Bin A", SEED_INSTANT));

        String assignmentId = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
            "/api/inventory/item-location-assignments"
        )
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {
                  "sku": " arc-location-100 ",
                  "itemLocationCode": " wh-source ",
                  "assignedLocationCode": " bin-a ",
                  "validFrom": "2026-03-02T00:00:00Z",
                  "assignedBy": " Inventory.Manager "
                }
                """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.inventoryItemId").isNotEmpty())
            .andExpect(jsonPath("$.sku").value("ARC-LOCATION-100"))
            .andExpect(jsonPath("$.itemLocationCode").value("WH-SOURCE"))
            .andExpect(jsonPath("$.assignedLocationCode").value("BIN-A"))
            .andExpect(jsonPath("$.validFrom").value("2026-03-02T00:00:00Z"))
            .andExpect(jsonPath("$.validThru").doesNotExist())
            .andExpect(jsonPath("$.active").value(true))
            .andExpect(jsonPath("$.assignedBy").value("inventory.manager"))
            .andExpect(jsonPath("$.assignedAt").isNotEmpty())
            .andReturn()
            .getResponse()
            .getContentAsString()
            .replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(get("/api/inventory/item-location-assignments/{id}", assignmentId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(assignmentId))
            .andExpect(jsonPath("$.assignedLocationCode").value("BIN-A"));

        mockMvc.perform(get("/api/inventory/item-location-assignments")
            .param("sku", "arc-location-100")
            .param("itemLocationCode", "wh-source")
            .param("assignedLocationCode", "bin-a")
            .param("active", "true")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].id").value(assignmentId))
            .andExpect(jsonPath("$.items[0].active").value(true));
    }

    @Test
    void endsInventoryItemLocationAssignmentAndListsEndHistory() throws Exception {
        String assignmentId = registerItemLocationAssignment(
            "arc-location-110",
            "wh-source",
            "bin-b",
            "2026-03-02T00:00:00Z"
        );

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
            "/api/inventory/item-location-assignments/{id}/end",
            assignmentId
        )
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {
                  "validThru": "2026-03-03T00:00:00Z",
                  "reason": "Moved to outbound",
                  "endedBy": " Inventory.Manager "
                }
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(assignmentId))
            .andExpect(jsonPath("$.validThru").value("2026-03-03T00:00:00Z"))
            .andExpect(jsonPath("$.active").value(false))
            .andExpect(jsonPath("$.endReason").value("Moved to outbound"))
            .andExpect(jsonPath("$.endedBy").value("inventory.manager"))
            .andExpect(jsonPath("$.endedAt").isNotEmpty());

        mockMvc.perform(get("/api/inventory/item-location-assignments/{id}/end-history", assignmentId)
            .param("endedBy", "inventory.manager")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].assignmentId").value(assignmentId))
            .andExpect(jsonPath("$.items[0].sku").value("ARC-LOCATION-110"))
            .andExpect(jsonPath("$.items[0].assignedLocationCode").value("BIN-B"))
            .andExpect(jsonPath("$.items[0].previousValidThru").doesNotExist())
            .andExpect(jsonPath("$.items[0].currentValidThru").value("2026-03-03T00:00:00Z"))
            .andExpect(jsonPath("$.items[0].reason").value("Moved to outbound"))
            .andExpect(jsonPath("$.items[0].endedBy").value("inventory.manager"));

        mockMvc.perform(get("/api/inventory/item-location-assignments")
            .param("assignedLocationCode", "bin-b")
            .param("active", "false")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].id").value(assignmentId));
    }

    @Test
    void rejectsEndingInventoryItemLocationAssignmentBeforeValidFrom() throws Exception {
        String assignmentId = registerItemLocationAssignment(
            "arc-location-111",
            "wh-source",
            "bin-c",
            "2026-03-02T00:00:00Z"
        );

        expectBadRequest(
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
                "/api/inventory/item-location-assignments/{id}/end",
                assignmentId
            )
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "validThru": "2026-03-01T00:00:00Z",
                      "reason": "Invalid window",
                      "endedBy": "inventory.manager"
                    }
                    """)),
            "validThru must be after or equal to validFrom",
            "/api/inventory/item-location-assignments/" + assignmentId + "/end"
        );
    }

    @Test
    void rejectsInventoryItemLocationAssignmentToInactiveLocation() throws Exception {
        inventoryItemRepository.save(InventoryItem.create(
            "arc-location-112",
            "wh-source",
            new BigDecimal("2"),
            SEED_INSTANT
        ));
        InventoryLocation inactiveLocation = inventoryLocationRepository.save(
            InventoryLocation.create("bin-inactive", "Inactive Bin", SEED_INSTANT)
        );
        inactiveLocation.setActive(false, SEED_INSTANT.plusSeconds(60));
        inventoryLocationRepository.save(inactiveLocation);

        expectBadRequest(
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                "/api/inventory/item-location-assignments"
            )
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "sku": "arc-location-112",
                      "itemLocationCode": "wh-source",
                      "assignedLocationCode": "bin-inactive",
                      "validFrom": "2026-03-02T00:00:00Z",
                      "assignedBy": "inventory.manager"
                    }
                    """)),
            "Inventory location is inactive: BIN-INACTIVE",
            "/api/inventory/item-location-assignments"
        );
    }

    @Test
    void createsReadsAndListsInventoryLocations() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/inventory/locations")
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {
                  "code": " wh-central ",
                  "name": " Central Warehouse ",
                  "facilityTypeCode": " warehouse ",
                  "addressLine1": " 100 Main Dock ",
                  "addressLine2": " Suite 2 ",
                  "city": " Salem ",
                  "regionCode": " or ",
                  "postalCode": "97301",
                  "countryCode": " us ",
                  "contactName": " Receiving Desk ",
                  "contactEmail": " Receiving@ArcanaERP.com "
                }
                """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.code").value("WH-CENTRAL"))
            .andExpect(jsonPath("$.name").value("Central Warehouse"))
            .andExpect(jsonPath("$.facilityTypeCode").value("WAREHOUSE"))
            .andExpect(jsonPath("$.addressLine1").value("100 Main Dock"))
            .andExpect(jsonPath("$.addressLine2").value("Suite 2"))
            .andExpect(jsonPath("$.city").value("Salem"))
            .andExpect(jsonPath("$.regionCode").value("OR"))
            .andExpect(jsonPath("$.postalCode").value("97301"))
            .andExpect(jsonPath("$.countryCode").value("US"))
            .andExpect(jsonPath("$.contactName").value("Receiving Desk"))
            .andExpect(jsonPath("$.contactEmail").value("receiving@arcanaerp.com"))
            .andExpect(jsonPath("$.active").value(true))
            .andExpect(jsonPath("$.createdAt").isNotEmpty())
            .andExpect(jsonPath("$.updatedAt").isNotEmpty());

        mockMvc.perform(get("/api/inventory/locations/{code}", "wh-central"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("WH-CENTRAL"))
            .andExpect(jsonPath("$.name").value("Central Warehouse"))
            .andExpect(jsonPath("$.facilityTypeCode").value("WAREHOUSE"))
            .andExpect(jsonPath("$.countryCode").value("US"))
            .andExpect(jsonPath("$.contactEmail").value("receiving@arcanaerp.com"))
            .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(get("/api/inventory/locations")
            .param("active", "true")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].code").value("WH-CENTRAL"))
            .andExpect(jsonPath("$.items[0].facilityTypeCode").value("WAREHOUSE"));

        mockMvc.perform(get("/api/inventory/locations")
            .param("active", "false")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(0))
            .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void createsReadsAndListsInventoryFacilities() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/inventory/facilities")
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {
                  "code": " dc-west ",
                  "name": " West Distribution Center ",
                  "facilityTypeCode": " warehouse ",
                  "addressLine1": " 200 Distribution Way ",
                  "addressLine2": " Building 4 ",
                  "city": " Reno ",
                  "regionCode": " nv ",
                  "postalCode": "89501",
                  "countryCode": " us ",
                  "contactName": " Facility Desk ",
                  "contactEmail": " Facility@ArcanaERP.com "
                }
                """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.code").value("DC-WEST"))
            .andExpect(jsonPath("$.name").value("West Distribution Center"))
            .andExpect(jsonPath("$.facilityTypeCode").value("WAREHOUSE"))
            .andExpect(jsonPath("$.addressLine1").value("200 Distribution Way"))
            .andExpect(jsonPath("$.addressLine2").value("Building 4"))
            .andExpect(jsonPath("$.city").value("Reno"))
            .andExpect(jsonPath("$.regionCode").value("NV"))
            .andExpect(jsonPath("$.postalCode").value("89501"))
            .andExpect(jsonPath("$.countryCode").value("US"))
            .andExpect(jsonPath("$.contactName").value("Facility Desk"))
            .andExpect(jsonPath("$.contactEmail").value("facility@arcanaerp.com"))
            .andExpect(jsonPath("$.active").value(true))
            .andExpect(jsonPath("$.createdAt").isNotEmpty())
            .andExpect(jsonPath("$.updatedAt").isNotEmpty());

        mockMvc.perform(get("/api/inventory/facilities/{code}", "dc-west"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("DC-WEST"))
            .andExpect(jsonPath("$.name").value("West Distribution Center"))
            .andExpect(jsonPath("$.facilityTypeCode").value("WAREHOUSE"))
            .andExpect(jsonPath("$.countryCode").value("US"));

        mockMvc.perform(get("/api/inventory/facilities")
            .param("active", "true")
            .param("query", "West")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].code").value("DC-WEST"))
            .andExpect(jsonPath("$.items[0].facilityTypeCode").value("WAREHOUSE"));
    }

    @Test
    void rejectsInventoryFacilityWithUnknownFacilityType() throws Exception {
        expectBadRequest(
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/inventory/facilities")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "code": "dc-yard",
                      "name": "Distribution Yard",
                      "facilityTypeCode": "yard"
                    }
                    """)),
            "Inventory location type not found: YARD",
            "/api/inventory/facilities"
        );
    }

    @Test
    void updatesInventoryFacilityActiveStateAndListsActiveHistory() throws Exception {
        inventoryFacilityRepository.save(
            InventoryFacility.create(
                "dc-retire",
                "Retirable Distribution Center",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                SEED_INSTANT
            )
        );

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
            "/api/inventory/facilities/{code}/active",
            "dc-retire"
        )
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {
                  "active": false,
                  "changedBy": " Facilities.Ops@ArcanaERP.com "
                }
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("DC-RETIRE"))
            .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/api/inventory/facilities")
            .param("active", "false")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].code").value("DC-RETIRE"));

        mockMvc.perform(get("/api/inventory/facilities/{code}/active-history", "dc-retire")
            .param("changedBy", "facilities.ops@arcanaerp.com")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].facilityCode").value("DC-RETIRE"))
            .andExpect(jsonPath("$.items[0].previousActive").value(true))
            .andExpect(jsonPath("$.items[0].currentActive").value(false))
            .andExpect(jsonPath("$.items[0].changedBy").value("facilities.ops@arcanaerp.com"))
            .andExpect(jsonPath("$.items[0].changedAt").isNotEmpty());
    }

    @Test
    void rejectsNoOpInventoryFacilityActiveStateChange() throws Exception {
        inventoryFacilityRepository.save(
            InventoryFacility.create(
                "dc-noop",
                "No-op Distribution Center",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                SEED_INSTANT
            )
        );

        expectBadRequest(
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
                "/api/inventory/facilities/{code}/active",
                "dc-noop"
            )
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "active": true,
                      "changedBy": "facilities.ops@arcanaerp.com"
                    }
                    """)),
            "Inventory facility active flag is already true",
            "/api/inventory/facilities/dc-noop/active"
        );
    }

    @Test
    void updatesInventoryFacilityMetadataAndListsMetadataHistory() throws Exception {
        inventoryFacilityRepository.save(
            InventoryFacility.create(
                "dc-metadata",
                "Metadata Distribution Center",
                "warehouse",
                "100 Dock Way",
                null,
                "Reno",
                "nv",
                "89501",
                "us",
                "Receiving",
                "receiving@arcanaerp.com",
                SEED_INSTANT
            )
        );

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
            "/api/inventory/facilities/{code}/metadata",
            "dc-metadata"
        )
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": " Metadata Distribution Center East ",
                  "facilityTypeCode": " store ",
                  "addressLine1": " 200 East Dock ",
                  "addressLine2": " Building 2 ",
                  "city": " Sparks ",
                  "regionCode": " nv ",
                  "postalCode": "89431",
                  "countryCode": " us ",
                  "contactName": " East Receiving ",
                  "contactEmail": " East.Receiving@ArcanaERP.com ",
                  "changedBy": " Facilities.Ops@ArcanaERP.com "
                }
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("DC-METADATA"))
            .andExpect(jsonPath("$.name").value("Metadata Distribution Center East"))
            .andExpect(jsonPath("$.facilityTypeCode").value("STORE"))
            .andExpect(jsonPath("$.addressLine1").value("200 East Dock"))
            .andExpect(jsonPath("$.addressLine2").value("Building 2"))
            .andExpect(jsonPath("$.city").value("Sparks"))
            .andExpect(jsonPath("$.regionCode").value("NV"))
            .andExpect(jsonPath("$.postalCode").value("89431"))
            .andExpect(jsonPath("$.countryCode").value("US"))
            .andExpect(jsonPath("$.contactName").value("East Receiving"))
            .andExpect(jsonPath("$.contactEmail").value("east.receiving@arcanaerp.com"));

        mockMvc.perform(get("/api/inventory/facilities/{code}/metadata-history", "dc-metadata")
            .param("changedBy", "facilities.ops@arcanaerp.com")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].facilityCode").value("DC-METADATA"))
            .andExpect(jsonPath("$.items[0].previousName").value("Metadata Distribution Center"))
            .andExpect(jsonPath("$.items[0].currentName").value("Metadata Distribution Center East"))
            .andExpect(jsonPath("$.items[0].previousFacilityTypeCode").value("WAREHOUSE"))
            .andExpect(jsonPath("$.items[0].currentFacilityTypeCode").value("STORE"))
            .andExpect(jsonPath("$.items[0].previousAddressLine1").value("100 Dock Way"))
            .andExpect(jsonPath("$.items[0].currentAddressLine1").value("200 East Dock"))
            .andExpect(jsonPath("$.items[0].previousCity").value("Reno"))
            .andExpect(jsonPath("$.items[0].currentCity").value("Sparks"))
            .andExpect(jsonPath("$.items[0].previousContactEmail").value("receiving@arcanaerp.com"))
            .andExpect(jsonPath("$.items[0].currentContactEmail").value("east.receiving@arcanaerp.com"))
            .andExpect(jsonPath("$.items[0].changedBy").value("facilities.ops@arcanaerp.com"))
            .andExpect(jsonPath("$.items[0].changedAt").isNotEmpty());
    }

    @Test
    void rejectsNoOpInventoryFacilityMetadataChange() throws Exception {
        inventoryFacilityRepository.save(
            InventoryFacility.create(
                "dc-metadata-noop",
                "No-op Metadata Distribution Center",
                "warehouse",
                "100 Dock Way",
                null,
                "Reno",
                "nv",
                "89501",
                "us",
                "Receiving",
                "receiving@arcanaerp.com",
                SEED_INSTANT
            )
        );

        expectBadRequest(
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
                "/api/inventory/facilities/{code}/metadata",
                "dc-metadata-noop"
            )
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "No-op Metadata Distribution Center",
                      "facilityTypeCode": "warehouse",
                      "addressLine1": "100 Dock Way",
                      "city": "Reno",
                      "regionCode": "nv",
                      "postalCode": "89501",
                      "countryCode": "us",
                      "contactName": "Receiving",
                      "contactEmail": "receiving@arcanaerp.com",
                      "changedBy": "facilities.ops@arcanaerp.com"
                    }
                    """)),
            "Inventory facility metadata is unchanged",
            "/api/inventory/facilities/dc-metadata-noop/metadata"
        );
    }

    @Test
    void rejectsInventoryFacilityMetadataUpdateWithUnknownFacilityType() throws Exception {
        inventoryFacilityRepository.save(
            InventoryFacility.create(
                "dc-metadata-type",
                "Typed Distribution Center",
                "warehouse",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                SEED_INSTANT
            )
        );

        expectBadRequest(
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
                "/api/inventory/facilities/{code}/metadata",
                "dc-metadata-type"
            )
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "Typed Distribution Center",
                      "facilityTypeCode": "yard",
                      "changedBy": "facilities.ops@arcanaerp.com"
                    }
                    """)),
            "Inventory location type not found: YARD",
            "/api/inventory/facilities/dc-metadata-type/metadata"
        );
    }

    @Test
    void createsReadsAndListsInventoryFacilityPartyRoleAssignments() throws Exception {
        inventoryFacilityRepository.save(
            InventoryFacility.create(
                "dc-role",
                "Role Distribution Center",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                SEED_INSTANT
            )
        );

        String assignmentId = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
            "/api/inventory/facility-party-role-assignments"
        )
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {
                  "facilityCode": " dc-role ",
                  "partyCode": " west-operator ",
                  "roleTypeCode": " manager ",
                  "comments": " Primary site manager ",
                  "fromDate": "2026-04-01T00:00:00Z",
                  "thruDate": "2026-12-31T23:59:59Z",
                  "assignedBy": " Facilities.Ops@ArcanaERP.com "
                }
                """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.inventoryFacilityId").isNotEmpty())
            .andExpect(jsonPath("$.facilityCode").value("DC-ROLE"))
            .andExpect(jsonPath("$.partyCode").value("WEST-OPERATOR"))
            .andExpect(jsonPath("$.roleTypeCode").value("MANAGER"))
            .andExpect(jsonPath("$.comments").value("Primary site manager"))
            .andExpect(jsonPath("$.fromDate").value("2026-04-01T00:00:00Z"))
            .andExpect(jsonPath("$.thruDate").value("2026-12-31T23:59:59Z"))
            .andExpect(jsonPath("$.assignedBy").value("facilities.ops@arcanaerp.com"))
            .andExpect(jsonPath("$.assignedAt").isNotEmpty())
            .andExpect(jsonPath("$.active").value(true))
            .andReturn()
            .getResponse()
            .getContentAsString()
            .replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(get("/api/inventory/facility-party-role-assignments/{id}", assignmentId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.facilityCode").value("DC-ROLE"))
            .andExpect(jsonPath("$.partyCode").value("WEST-OPERATOR"))
            .andExpect(jsonPath("$.roleTypeCode").value("MANAGER"));

        mockMvc.perform(get("/api/inventory/facility-party-role-assignments")
            .param("facilityCode", "dc-role")
            .param("partyCode", "west-operator")
            .param("roleTypeCode", "manager")
            .param("assignedBy", "facilities.ops@arcanaerp.com")
            .param("active", "true")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].facilityCode").value("DC-ROLE"))
            .andExpect(jsonPath("$.items[0].partyCode").value("WEST-OPERATOR"))
            .andExpect(jsonPath("$.items[0].roleTypeCode").value("MANAGER"));
    }

    @Test
    void endsInventoryFacilityPartyRoleAssignmentAndListsEndHistory() throws Exception {
        inventoryFacilityRepository.save(
            InventoryFacility.create(
                "dc-role-end",
                "End Role Distribution Center",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                SEED_INSTANT
            )
        );

        String assignmentId = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
            "/api/inventory/facility-party-role-assignments"
        )
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {
                  "facilityCode": "dc-role-end",
                  "partyCode": "west-operator",
                  "roleTypeCode": "manager",
                  "fromDate": "2026-04-01T00:00:00Z",
                  "assignedBy": "facilities.ops@arcanaerp.com"
                }
                """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.active").value(true))
            .andReturn()
            .getResponse()
            .getContentAsString()
            .replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
            "/api/inventory/facility-party-role-assignments/{id}/end",
            assignmentId
        )
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {
                  "thruDate": "2026-09-01T00:00:00Z",
                  "reason": " Operator rotation ",
                  "endedBy": " Facilities.Ops@ArcanaERP.com "
                }
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(assignmentId))
            .andExpect(jsonPath("$.active").value(false))
            .andExpect(jsonPath("$.thruDate").value("2026-09-01T00:00:00Z"))
            .andExpect(jsonPath("$.endReason").value("Operator rotation"))
            .andExpect(jsonPath("$.endedBy").value("facilities.ops@arcanaerp.com"))
            .andExpect(jsonPath("$.endedAt").isNotEmpty());

        mockMvc.perform(get("/api/inventory/facility-party-role-assignments")
            .param("facilityCode", "dc-role-end")
            .param("active", "false")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].active").value(false));

        mockMvc.perform(get("/api/inventory/facility-party-role-assignments/{id}/end-history", assignmentId)
            .param("endedBy", "facilities.ops@arcanaerp.com")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].assignmentId").value(assignmentId))
            .andExpect(jsonPath("$.items[0].facilityCode").value("DC-ROLE-END"))
            .andExpect(jsonPath("$.items[0].partyCode").value("WEST-OPERATOR"))
            .andExpect(jsonPath("$.items[0].roleTypeCode").value("MANAGER"))
            .andExpect(jsonPath("$.items[0].previousThruDate").doesNotExist())
            .andExpect(jsonPath("$.items[0].currentThruDate").value("2026-09-01T00:00:00Z"))
            .andExpect(jsonPath("$.items[0].reason").value("Operator rotation"))
            .andExpect(jsonPath("$.items[0].endedBy").value("facilities.ops@arcanaerp.com"))
            .andExpect(jsonPath("$.items[0].endedAt").isNotEmpty());

        expectBadRequest(
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
                "/api/inventory/facility-party-role-assignments/{id}/end",
                assignmentId
            )
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "thruDate": "2026-09-02T00:00:00Z",
                      "reason": "Second end",
                      "endedBy": "facilities.ops@arcanaerp.com"
                    }
                    """)),
            "Inventory facility party role assignment is already ended",
            "/api/inventory/facility-party-role-assignments/" + assignmentId + "/end"
        );
    }

    @Test
    void rejectsDuplicateInventoryFacilityPartyRoleAssignment() throws Exception {
        inventoryFacilityRepository.save(
            InventoryFacility.create(
                "dc-role-dup",
                "Duplicate Role Distribution Center",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                SEED_INSTANT
            )
        );
        String payload = """
            {
              "facilityCode": "dc-role-dup",
              "partyCode": "west-operator",
              "roleTypeCode": "manager",
              "assignedBy": "facilities.ops@arcanaerp.com"
            }
            """;
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
            "/api/inventory/facility-party-role-assignments"
        )
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content(payload))
            .andExpect(status().isCreated());

        expectConflict(
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                "/api/inventory/facility-party-role-assignments"
            )
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(payload)),
            "Inventory facility party role assignment already exists for facility: DC-ROLE-DUP, party: WEST-OPERATOR, role type: MANAGER",
            "/api/inventory/facility-party-role-assignments"
        );
    }

    @Test
    void rejectsUnknownFacilityForPartyRoleAssignment() throws Exception {
        expectInventoryFacilityNotFound(
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                "/api/inventory/facility-party-role-assignments"
            )
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "facilityCode": "missing-dc",
                      "partyCode": "west-operator",
                      "roleTypeCode": "manager",
                      "assignedBy": "facilities.ops@arcanaerp.com"
                    }
                    """)),
            "MISSING-DC",
            "/api/inventory/facility-party-role-assignments"
        );
    }

    @Test
    void rejectsUnknownPartyForFacilityPartyRoleAssignment() throws Exception {
        inventoryFacilityRepository.save(
            InventoryFacility.create(
                "dc-role-party-missing",
                "Missing Party Distribution Center",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                SEED_INSTANT
            )
        );

        expectBadRequest(
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                "/api/inventory/facility-party-role-assignments"
            )
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "facilityCode": "dc-role-party-missing",
                      "partyCode": "unknown-party",
                      "roleTypeCode": "manager",
                      "assignedBy": "facilities.ops@arcanaerp.com"
                    }
                    """)),
            "Inventory party not found: UNKNOWN-PARTY",
            "/api/inventory/facility-party-role-assignments"
        );
    }

    @Test
    void rejectsUnknownPartyRoleTypeForFacilityPartyRoleAssignment() throws Exception {
        inventoryFacilityRepository.save(
            InventoryFacility.create(
                "dc-role-type-missing",
                "Missing Role Type Distribution Center",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                SEED_INSTANT
            )
        );

        expectBadRequest(
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                "/api/inventory/facility-party-role-assignments"
            )
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "facilityCode": "dc-role-type-missing",
                      "partyCode": "west-operator",
                      "roleTypeCode": "unknown-role",
                      "assignedBy": "facilities.ops@arcanaerp.com"
                    }
                    """)),
            "Inventory party role type not found: UNKNOWN-ROLE",
            "/api/inventory/facility-party-role-assignments"
        );
    }

    @Test
    void rejectsInactiveFacilityForPartyRoleAssignment() throws Exception {
        InventoryFacility facility = inventoryFacilityRepository.save(
            InventoryFacility.create(
                "dc-role-inactive",
                "Inactive Role Distribution Center",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                SEED_INSTANT
            )
        );
        facility.setActive(false, SEED_INSTANT.plusSeconds(60));
        inventoryFacilityRepository.save(facility);

        expectBadRequest(
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                "/api/inventory/facility-party-role-assignments"
            )
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "facilityCode": "dc-role-inactive",
                      "partyCode": "west-operator",
                      "roleTypeCode": "manager",
                      "assignedBy": "facilities.ops@arcanaerp.com"
                    }
                    """)),
            "Inventory facility is inactive: DC-ROLE-INACTIVE",
            "/api/inventory/facility-party-role-assignments"
        );
    }

    @Test
    void rejectsInvalidFacilityPartyRoleAssignmentDateWindow() throws Exception {
        inventoryFacilityRepository.save(
            InventoryFacility.create(
                "dc-role-window",
                "Window Role Distribution Center",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                SEED_INSTANT
            )
        );

        expectBadRequest(
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                "/api/inventory/facility-party-role-assignments"
            )
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "facilityCode": "dc-role-window",
                      "partyCode": "west-operator",
                      "roleTypeCode": "manager",
                      "fromDate": "2026-12-31T23:59:59Z",
                      "thruDate": "2026-04-01T00:00:00Z",
                      "assignedBy": "facilities.ops@arcanaerp.com"
                    }
                    """)),
            "fromDate must be before or equal to thruDate",
            "/api/inventory/facility-party-role-assignments"
        );
    }

    @Test
    void createsReadsAndListsInventoryFixedAssets() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/inventory/fixed-assets")
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {
                  "code": " truck-99 ",
                  "description": " Delivery Truck 99 ",
                  "fixedAssetTypeCode": " vehicle ",
                  "comments": " Refrigerated box truck ",
                  "externalIdentifier": "FA-0099",
                  "externalIdSource": " legacy "
                }
                """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.code").value("TRUCK-99"))
            .andExpect(jsonPath("$.description").value("Delivery Truck 99"))
            .andExpect(jsonPath("$.fixedAssetTypeCode").value("VEHICLE"))
            .andExpect(jsonPath("$.comments").value("Refrigerated box truck"))
            .andExpect(jsonPath("$.externalIdentifier").value("FA-0099"))
            .andExpect(jsonPath("$.externalIdSource").value("LEGACY"))
            .andExpect(jsonPath("$.active").value(true))
            .andExpect(jsonPath("$.createdAt").isNotEmpty())
            .andExpect(jsonPath("$.updatedAt").isNotEmpty());

        mockMvc.perform(get("/api/inventory/fixed-assets/{code}", "truck-99"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("TRUCK-99"))
            .andExpect(jsonPath("$.description").value("Delivery Truck 99"))
            .andExpect(jsonPath("$.fixedAssetTypeCode").value("VEHICLE"));

        mockMvc.perform(get("/api/inventory/fixed-assets")
            .param("active", "true")
            .param("query", "Delivery")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].code").value("TRUCK-99"));
    }

    @Test
    void rejectsUnknownInventoryFixedAssetTypeCode() throws Exception {
        expectInventoryFixedAssetTypeNotFound(
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/inventory/fixed-assets")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "code": " truck-100 ",
                      "description": " Delivery Truck 100 ",
                      "fixedAssetTypeCode": " trailer "
                    }
                    """)),
            "TRAILER",
            "/api/inventory/fixed-assets"
        );
    }

    @Test
    void updatesInventoryFixedAssetActiveStateAndListsActiveHistory() throws Exception {
        inventoryFixedAssetRepository.save(
            InventoryFixedAsset.create(
                "truck-101",
                "Delivery Truck 101",
                "vehicle",
                null,
                null,
                null,
                SEED_INSTANT
            )
        );

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
            "/api/inventory/fixed-assets/{code}/active",
            "truck-101"
        )
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {
                  "active": false,
                  "changedBy": " Fleet.Manager@ArcanaERP.com "
                }
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("TRUCK-101"))
            .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/api/inventory/fixed-assets")
            .param("active", "false")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].code").value("TRUCK-101"));

        mockMvc.perform(get("/api/inventory/fixed-assets/{code}/active-history", "truck-101")
            .param("changedBy", "fleet.manager@arcanaerp.com")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].fixedAssetCode").value("TRUCK-101"))
            .andExpect(jsonPath("$.items[0].previousActive").value(true))
            .andExpect(jsonPath("$.items[0].currentActive").value(false))
            .andExpect(jsonPath("$.items[0].changedBy").value("fleet.manager@arcanaerp.com"))
            .andExpect(jsonPath("$.items[0].changedAt").isNotEmpty());
    }

    @Test
    void rejectsNoOpInventoryFixedAssetActiveStateChange() throws Exception {
        inventoryFixedAssetRepository.save(
            InventoryFixedAsset.create(
                "truck-102",
                "Delivery Truck 102",
                "vehicle",
                null,
                null,
                null,
                SEED_INSTANT
            )
        );

        expectBadRequest(
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
                "/api/inventory/fixed-assets/{code}/active",
                "truck-102"
            )
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "active": true,
                      "changedBy": "fleet.manager@arcanaerp.com"
                    }
                    """)),
            "Inventory fixed asset active flag is already true",
            "/api/inventory/fixed-assets/truck-102/active"
        );
    }

    @Test
    void updatesInventoryFixedAssetMetadataAndListsMetadataHistory() throws Exception {
        seedFixedAssetType("FORKLIFT", "Forklift");
        inventoryFixedAssetRepository.save(
            InventoryFixedAsset.create(
                "truck-103",
                "Delivery Truck 103",
                "vehicle",
                "Box truck",
                "FA-0103",
                "legacy",
                SEED_INSTANT
            )
        );

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
            "/api/inventory/fixed-assets/{code}/metadata",
            "truck-103"
        )
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {
                  "description": " Forklift 103 ",
                  "fixedAssetTypeCode": " forklift ",
                  "comments": " Warehouse forklift ",
                  "externalIdentifier": "FA-1103",
                  "externalIdSource": " fleet ",
                  "changedBy": " Fleet.Manager@ArcanaERP.com "
                }
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("TRUCK-103"))
            .andExpect(jsonPath("$.description").value("Forklift 103"))
            .andExpect(jsonPath("$.fixedAssetTypeCode").value("FORKLIFT"))
            .andExpect(jsonPath("$.comments").value("Warehouse forklift"))
            .andExpect(jsonPath("$.externalIdentifier").value("FA-1103"))
            .andExpect(jsonPath("$.externalIdSource").value("FLEET"));

        mockMvc.perform(get("/api/inventory/fixed-assets/{code}/metadata-history", "truck-103")
            .param("changedBy", "fleet.manager@arcanaerp.com")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].fixedAssetCode").value("TRUCK-103"))
            .andExpect(jsonPath("$.items[0].previousDescription").value("Delivery Truck 103"))
            .andExpect(jsonPath("$.items[0].currentDescription").value("Forklift 103"))
            .andExpect(jsonPath("$.items[0].previousFixedAssetTypeCode").value("VEHICLE"))
            .andExpect(jsonPath("$.items[0].currentFixedAssetTypeCode").value("FORKLIFT"))
            .andExpect(jsonPath("$.items[0].previousComments").value("Box truck"))
            .andExpect(jsonPath("$.items[0].currentComments").value("Warehouse forklift"))
            .andExpect(jsonPath("$.items[0].previousExternalIdentifier").value("FA-0103"))
            .andExpect(jsonPath("$.items[0].currentExternalIdentifier").value("FA-1103"))
            .andExpect(jsonPath("$.items[0].previousExternalIdSource").value("LEGACY"))
            .andExpect(jsonPath("$.items[0].currentExternalIdSource").value("FLEET"))
            .andExpect(jsonPath("$.items[0].changedBy").value("fleet.manager@arcanaerp.com"))
            .andExpect(jsonPath("$.items[0].changedAt").isNotEmpty());
    }

    @Test
    void rejectsNoOpInventoryFixedAssetMetadataChange() throws Exception {
        inventoryFixedAssetRepository.save(
            InventoryFixedAsset.create(
                "truck-104",
                "Delivery Truck 104",
                "vehicle",
                "Box truck",
                "FA-0104",
                "legacy",
                SEED_INSTANT
            )
        );

        expectBadRequest(
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
                "/api/inventory/fixed-assets/{code}/metadata",
                "truck-104"
            )
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "description": " Delivery Truck 104 ",
                      "fixedAssetTypeCode": " vehicle ",
                      "comments": " Box truck ",
                      "externalIdentifier": "FA-0104",
                      "externalIdSource": " legacy ",
                      "changedBy": "fleet.manager@arcanaerp.com"
                    }
                    """)),
            "Inventory fixed asset metadata is unchanged",
            "/api/inventory/fixed-assets/truck-104/metadata"
        );
    }

    @Test
    void rejectsUnknownInventoryFixedAssetTypeCodeOnMetadataUpdate() throws Exception {
        inventoryFixedAssetRepository.save(
            InventoryFixedAsset.create(
                "truck-105",
                "Delivery Truck 105",
                "vehicle",
                null,
                null,
                null,
                SEED_INSTANT
            )
        );

        expectInventoryFixedAssetTypeNotFound(
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
                "/api/inventory/fixed-assets/{code}/metadata",
                "truck-105"
            )
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "description": " Trailer 105 ",
                      "fixedAssetTypeCode": " trailer ",
                      "changedBy": "fleet.manager@arcanaerp.com"
                    }
                    """)),
            "TRAILER",
            "/api/inventory/fixed-assets/truck-105/metadata"
        );
    }

    @Test
    void createsReadsAndListsInventoryFixedAssetPartyRoleAssignments() throws Exception {
        inventoryFixedAssetRepository.save(
            InventoryFixedAsset.create(
                "truck-106",
                "Delivery Truck 106",
                "vehicle",
                null,
                null,
                null,
                SEED_INSTANT
            )
        );

        String response = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
            "/api/inventory/fixed-asset-party-role-assignments"
        )
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {
                  "fixedAssetCode": " truck-106 ",
                  "partyCode": " west-carrier ",
                  "roleTypeCode": " operator ",
                  "comments": " Primary carrier operator ",
                  "fromDate": "2026-04-01T00:00:00Z",
                  "thruDate": "2026-12-31T23:59:59Z",
                  "assignedBy": " Fleet.Manager@ArcanaERP.com "
                }
                """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.inventoryFixedAssetId").isNotEmpty())
            .andExpect(jsonPath("$.fixedAssetCode").value("TRUCK-106"))
            .andExpect(jsonPath("$.partyCode").value("WEST-CARRIER"))
            .andExpect(jsonPath("$.roleTypeCode").value("OPERATOR"))
            .andExpect(jsonPath("$.comments").value("Primary carrier operator"))
            .andExpect(jsonPath("$.fromDate").value("2026-04-01T00:00:00Z"))
            .andExpect(jsonPath("$.thruDate").value("2026-12-31T23:59:59Z"))
            .andExpect(jsonPath("$.assignedBy").value("fleet.manager@arcanaerp.com"))
            .andExpect(jsonPath("$.assignedAt").isNotEmpty())
            .andExpect(jsonPath("$.active").value(true))
            .andReturn()
            .getResponse()
            .getContentAsString();

        String assignmentId = response.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(get("/api/inventory/fixed-asset-party-role-assignments/{id}", assignmentId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.fixedAssetCode").value("TRUCK-106"))
            .andExpect(jsonPath("$.partyCode").value("WEST-CARRIER"))
            .andExpect(jsonPath("$.roleTypeCode").value("OPERATOR"));

        mockMvc.perform(get("/api/inventory/fixed-asset-party-role-assignments")
            .param("fixedAssetCode", "truck-106")
            .param("partyCode", "west-carrier")
            .param("roleTypeCode", "operator")
            .param("assignedBy", "fleet.manager@arcanaerp.com")
            .param("active", "true")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].fixedAssetCode").value("TRUCK-106"))
            .andExpect(jsonPath("$.items[0].partyCode").value("WEST-CARRIER"))
            .andExpect(jsonPath("$.items[0].roleTypeCode").value("OPERATOR"));
    }

    @Test
    void endsInventoryFixedAssetPartyRoleAssignmentAndListsEndHistory() throws Exception {
        inventoryFixedAssetRepository.save(
            InventoryFixedAsset.create(
                "truck-106-end",
                "Delivery Truck 106 End",
                "vehicle",
                null,
                null,
                null,
                SEED_INSTANT
            )
        );

        String response = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
            "/api/inventory/fixed-asset-party-role-assignments"
        )
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {
                  "fixedAssetCode": "truck-106-end",
                  "partyCode": "west-carrier",
                  "roleTypeCode": "operator",
                  "fromDate": "2026-04-01T00:00:00Z",
                  "assignedBy": "fleet.manager@arcanaerp.com"
                }
                """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.active").value(true))
            .andReturn()
            .getResponse()
            .getContentAsString();

        String assignmentId = response.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
            "/api/inventory/fixed-asset-party-role-assignments/{id}/end",
            assignmentId
        )
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {
                  "thruDate": "2026-09-01T00:00:00Z",
                  "reason": " Carrier rotation ",
                  "endedBy": " Fleet.Manager@ArcanaERP.com "
                }
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(assignmentId))
            .andExpect(jsonPath("$.active").value(false))
            .andExpect(jsonPath("$.thruDate").value("2026-09-01T00:00:00Z"))
            .andExpect(jsonPath("$.endReason").value("Carrier rotation"))
            .andExpect(jsonPath("$.endedBy").value("fleet.manager@arcanaerp.com"))
            .andExpect(jsonPath("$.endedAt").isNotEmpty());

        mockMvc.perform(get("/api/inventory/fixed-asset-party-role-assignments")
            .param("fixedAssetCode", "truck-106-end")
            .param("active", "false")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].active").value(false));

        mockMvc.perform(get("/api/inventory/fixed-asset-party-role-assignments/{id}/end-history", assignmentId)
            .param("endedBy", "fleet.manager@arcanaerp.com")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].assignmentId").value(assignmentId))
            .andExpect(jsonPath("$.items[0].fixedAssetCode").value("TRUCK-106-END"))
            .andExpect(jsonPath("$.items[0].partyCode").value("WEST-CARRIER"))
            .andExpect(jsonPath("$.items[0].roleTypeCode").value("OPERATOR"))
            .andExpect(jsonPath("$.items[0].previousThruDate").doesNotExist())
            .andExpect(jsonPath("$.items[0].currentThruDate").value("2026-09-01T00:00:00Z"))
            .andExpect(jsonPath("$.items[0].reason").value("Carrier rotation"))
            .andExpect(jsonPath("$.items[0].endedBy").value("fleet.manager@arcanaerp.com"))
            .andExpect(jsonPath("$.items[0].endedAt").isNotEmpty());
    }

    @Test
    void rejectsDuplicateInventoryFixedAssetPartyRoleAssignment() throws Exception {
        inventoryFixedAssetRepository.save(
            InventoryFixedAsset.create(
                "truck-107",
                "Delivery Truck 107",
                "vehicle",
                null,
                null,
                null,
                SEED_INSTANT
            )
        );
        String payload = """
            {
              "fixedAssetCode": "truck-107",
              "partyCode": "west-carrier",
              "roleTypeCode": "operator",
              "assignedBy": "fleet.manager@arcanaerp.com"
            }
            """;
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
            "/api/inventory/fixed-asset-party-role-assignments"
        )
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content(payload))
            .andExpect(status().isCreated());

        expectConflict(
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                "/api/inventory/fixed-asset-party-role-assignments"
            )
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(payload)),
            "Inventory fixed asset party role assignment already exists for fixed asset: TRUCK-107, party: WEST-CARRIER, role type: OPERATOR",
            "/api/inventory/fixed-asset-party-role-assignments"
        );
    }

    @Test
    void rejectsUnknownFixedAssetForPartyRoleAssignment() throws Exception {
        expectInventoryFixedAssetNotFound(
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                "/api/inventory/fixed-asset-party-role-assignments"
            )
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fixedAssetCode": "missing-truck",
                      "partyCode": "west-carrier",
                      "roleTypeCode": "operator",
                      "assignedBy": "fleet.manager@arcanaerp.com"
                    }
                    """)),
            "MISSING-TRUCK",
            "/api/inventory/fixed-asset-party-role-assignments"
        );
    }

    @Test
    void rejectsUnknownPartyForFixedAssetPartyRoleAssignment() throws Exception {
        inventoryFixedAssetRepository.save(
            InventoryFixedAsset.create(
                "truck-party-missing",
                "Truck Party Missing",
                "vehicle",
                null,
                null,
                null,
                SEED_INSTANT
            )
        );

        expectBadRequest(
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                "/api/inventory/fixed-asset-party-role-assignments"
            )
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fixedAssetCode": "truck-party-missing",
                      "partyCode": "unknown-party",
                      "roleTypeCode": "operator",
                      "assignedBy": "fleet.manager@arcanaerp.com"
                    }
                    """)),
            "Inventory party not found: UNKNOWN-PARTY",
            "/api/inventory/fixed-asset-party-role-assignments"
        );
    }

    @Test
    void rejectsUnknownPartyRoleTypeForFixedAssetPartyRoleAssignment() throws Exception {
        inventoryFixedAssetRepository.save(
            InventoryFixedAsset.create(
                "truck-role-missing",
                "Truck Role Missing",
                "vehicle",
                null,
                null,
                null,
                SEED_INSTANT
            )
        );

        expectBadRequest(
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                "/api/inventory/fixed-asset-party-role-assignments"
            )
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fixedAssetCode": "truck-role-missing",
                      "partyCode": "west-carrier",
                      "roleTypeCode": "unknown-role",
                      "assignedBy": "fleet.manager@arcanaerp.com"
                    }
                    """)),
            "Inventory party role type not found: UNKNOWN-ROLE",
            "/api/inventory/fixed-asset-party-role-assignments"
        );
    }

    @Test
    void rejectsInvalidFixedAssetPartyRoleAssignmentDateWindow() throws Exception {
        inventoryFixedAssetRepository.save(
            InventoryFixedAsset.create(
                "truck-108",
                "Delivery Truck 108",
                "vehicle",
                null,
                null,
                null,
                SEED_INSTANT
            )
        );

        expectBadRequest(
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                "/api/inventory/fixed-asset-party-role-assignments"
            )
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fixedAssetCode": "truck-108",
                      "partyCode": "west-carrier",
                      "roleTypeCode": "operator",
                      "fromDate": "2026-12-31T23:59:59Z",
                      "thruDate": "2026-04-01T00:00:00Z",
                      "assignedBy": "fleet.manager@arcanaerp.com"
                    }
                    """)),
            "fromDate must be before or equal to thruDate",
            "/api/inventory/fixed-asset-party-role-assignments"
        );
    }

    @Test
    void rejectsDuplicateInventoryLocationCode() throws Exception {
        String payload = """
            {
              "code": "wh-central",
              "name": "Central Warehouse"
            }
            """;
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/inventory/locations")
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content(payload))
            .andExpect(status().isCreated());

        expectConflict(
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/inventory/locations")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(payload)),
            "Inventory location already exists for code: WH-CENTRAL",
            "/api/inventory/locations"
        );
    }

    @Test
    void rejectsInventoryLocationRegistrationWithUnknownFacilityType() throws Exception {
        expectBadRequest(
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/inventory/locations")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "code": "wh-unknown-type",
                      "name": "Unknown Type Warehouse",
                      "facilityTypeCode": "yard"
                    }
                    """)),
            "Inventory location type not found: YARD",
            "/api/inventory/locations"
        );
    }

    @Test
    void returnsNotFoundForUnknownInventoryLocation() throws Exception {
        expectInventoryLocationNotFound(
            mockMvc.perform(get("/api/inventory/locations/{code}", "wh-missing")),
            "WH-MISSING",
            "/api/inventory/locations/wh-missing"
        );
    }

    @Test
    void updatesInventoryLocationMetadataWithoutChangingActiveState() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/inventory/locations")
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {
                  "code": "wh-metadata",
                  "name": "Metadata Warehouse"
                }
                """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
            "/api/inventory/locations/{code}/metadata",
            "wh-metadata"
        )
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": "Metadata Warehouse East",
                  "facilityTypeCode": "store",
                  "addressLine1": "500 East Dock",
                  "city": "Portland",
                  "regionCode": "or",
                  "postalCode": "97201",
                  "countryCode": "us",
                  "contactName": "East Receiving",
                  "contactEmail": "East.Receiving@ArcanaERP.com",
                  "changedBy": " Facilities.Ops@ArcanaERP.com "
                }
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("WH-METADATA"))
            .andExpect(jsonPath("$.name").value("Metadata Warehouse East"))
            .andExpect(jsonPath("$.facilityTypeCode").value("STORE"))
            .andExpect(jsonPath("$.addressLine1").value("500 East Dock"))
            .andExpect(jsonPath("$.city").value("Portland"))
            .andExpect(jsonPath("$.regionCode").value("OR"))
            .andExpect(jsonPath("$.postalCode").value("97201"))
            .andExpect(jsonPath("$.countryCode").value("US"))
            .andExpect(jsonPath("$.contactName").value("East Receiving"))
            .andExpect(jsonPath("$.contactEmail").value("east.receiving@arcanaerp.com"))
            .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(get("/api/inventory/locations/{code}/metadata-history", "wh-metadata")
            .param("changedBy", "facilities.ops@arcanaerp.com")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].locationCode").value("WH-METADATA"))
            .andExpect(jsonPath("$.items[0].previousName").value("Metadata Warehouse"))
            .andExpect(jsonPath("$.items[0].currentName").value("Metadata Warehouse East"))
            .andExpect(jsonPath("$.items[0].currentFacilityTypeCode").value("STORE"))
            .andExpect(jsonPath("$.items[0].currentCountryCode").value("US"))
            .andExpect(jsonPath("$.items[0].currentContactEmail").value("east.receiving@arcanaerp.com"))
            .andExpect(jsonPath("$.items[0].changedBy").value("facilities.ops@arcanaerp.com"))
            .andExpect(jsonPath("$.items[0].changedAt").isNotEmpty());
    }

    @Test
    void rejectsInventoryLocationMetadataUpdateWithUnknownFacilityType() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/inventory/locations")
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {
                  "code": "wh-unknown-type-update",
                  "name": "Unknown Type Update Warehouse"
                }
                """))
            .andExpect(status().isCreated());

        expectBadRequest(
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
                "/api/inventory/locations/{code}/metadata",
                "wh-unknown-type-update"
            )
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "Unknown Type Update Warehouse",
                      "facilityTypeCode": "yard",
                      "changedBy": "facilities.ops@arcanaerp.com"
                    }
                    """)),
            "Inventory location type not found: YARD",
            "/api/inventory/locations/wh-unknown-type-update/metadata"
        );
    }

    @Test
    void updatesInventoryLocationActiveStateAndFiltersByLifecycleState() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/inventory/locations")
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {
                  "code": "wh-retire",
                  "name": "Retirable Warehouse"
                }
                """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/inventory/locations/{code}/active", "wh-retire")
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {
                  "active": false
                }
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("WH-RETIRE"))
            .andExpect(jsonPath("$.active").value(false))
            .andExpect(jsonPath("$.updatedAt").isNotEmpty());

        mockMvc.perform(get("/api/inventory/locations")
            .param("active", "false")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].code").value("WH-RETIRE"))
            .andExpect(jsonPath("$.items[0].active").value(false));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/inventory/locations/{code}/active", "wh-retire")
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {
                  "active": true
                }
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("WH-RETIRE"))
            .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void rejectsNoOpInventoryLocationActiveStateChange() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/inventory/locations")
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {
                  "code": "wh-noop",
                  "name": "No-op Warehouse"
                }
                """))
            .andExpect(status().isCreated());

        expectBadRequest(
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/inventory/locations/{code}/active", "wh-noop")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "active": true
                    }
                    """)),
            "Inventory location active flag is already true",
            "/api/inventory/locations/wh-noop/active"
        );
    }

    @Test
    void rejectsNoOpInventoryLocationMetadataChange() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/inventory/locations")
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {
                  "code": "wh-metadata-noop",
                  "name": "No-op Metadata Warehouse",
                  "facilityTypeCode": "warehouse"
                }
                """))
            .andExpect(status().isCreated());

        expectBadRequest(
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
                "/api/inventory/locations/{code}/metadata",
                "wh-metadata-noop"
            )
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "No-op Metadata Warehouse",
                      "facilityTypeCode": "warehouse",
                      "changedBy": "facilities.ops@arcanaerp.com"
                    }
                    """)),
            "Inventory location metadata is unchanged",
            "/api/inventory/locations/wh-metadata-noop/metadata"
        );
    }

    @Test
    void filtersInventoryLocationMetadataHistoryByChangedAtRange() throws Exception {
        InventoryLocation location = inventoryLocationRepository.save(
            InventoryLocation.create("wh-location-history", "Location History", SEED_INSTANT)
        );
        locationMetadataChangeAuditRepository.save(InventoryLocationMetadataChangeAudit.create(
            location.getId(),
            "wh-location-history",
            new InventoryLocationMetadataSnapshot(
                "Location History",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
            ),
            new InventoryLocationMetadataSnapshot(
                "Location History East",
                "warehouse",
                "100 East Dock",
                null,
                "Salem",
                "or",
                "97301",
                "us",
                "Receiving",
                "receiving@arcanaerp.com"
            ),
            "facilities.ops@arcanaerp.com",
            Instant.parse("2026-03-01T01:00:00Z")
        ));
        locationMetadataChangeAuditRepository.save(InventoryLocationMetadataChangeAudit.create(
            location.getId(),
            "wh-location-history",
            new InventoryLocationMetadataSnapshot(
                "Location History East",
                "warehouse",
                "100 East Dock",
                null,
                "Salem",
                "or",
                "97301",
                "us",
                "Receiving",
                "receiving@arcanaerp.com"
            ),
            new InventoryLocationMetadataSnapshot(
                "Location History West",
                "store",
                "200 West Dock",
                null,
                "Portland",
                "or",
                "97201",
                "us",
                "West Receiving",
                "west.receiving@arcanaerp.com"
            ),
            "planning@arcanaerp.com",
            Instant.parse("2026-03-02T01:00:00Z")
        ));

        mockMvc.perform(get("/api/inventory/locations/{code}/metadata-history", "wh-location-history")
            .param("changedAtFrom", "2026-03-02T00:00:00Z")
            .param("changedAtTo", "2026-03-02T23:59:59Z")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].currentName").value("Location History West"))
            .andExpect(jsonPath("$.items[0].currentFacilityTypeCode").value("STORE"))
            .andExpect(jsonPath("$.items[0].changedBy").value("planning@arcanaerp.com"));
    }

    @Test
    void createsReadsListsAndUpdatesInventoryItems() throws Exception {
        UserView owner = userDirectory.registerUser(
            new RegisterUserCommand(
                "inventoryown01",
                "Inventory Owner 01",
                "keeper",
                "Keeper",
                "owner01@inventory.example",
                "Owner 01"
            )
        );
        UserView nextOwner = userDirectory.registerUser(
            new RegisterUserCommand(
                "inventoryown01",
                "Inventory Owner 01",
                "manager",
                "Manager",
                "manager01@inventory.example",
                "Manager 01"
            )
        );

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/inventory/items")
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {
                  "sku": " arc-9250 ",
                  "locationCode": " wh-item ",
                  "onHandQuantity": 14,
                  "availableQuantity": 12,
                  "soldQuantity": 2,
                  "unitOfMeasurementCode": "case",
                  "classificationCode": "quarantine",
                  "productInstanceCode": " pi-100 ",
                  "externalReference": " legacy-entry-100 ",
                  "sourceSystemCode": " legacy_inv ",
                  "ownerTenantCode": " inventoryown01 ",
                  "ownerUserId": "%s",
                  "ownerRoleCode": " keeper "
                }
                """.formatted(owner.id())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.sku").value("ARC-9250"))
            .andExpect(jsonPath("$.locationCode").value("WH-ITEM"))
            .andExpect(jsonPath("$.onHandQuantity").value(14))
            .andExpect(jsonPath("$.availableQuantity").value(12))
            .andExpect(jsonPath("$.soldQuantity").value(2))
            .andExpect(jsonPath("$.unitOfMeasurementCode").value("CASE"))
            .andExpect(jsonPath("$.classificationCode").value("QUARANTINE"))
            .andExpect(jsonPath("$.productInstanceCode").value("PI-100"))
            .andExpect(jsonPath("$.externalReference").value("legacy-entry-100"))
            .andExpect(jsonPath("$.sourceSystemCode").value("LEGACY_INV"))
            .andExpect(jsonPath("$.ownerTenantCode").value("INVENTORYOWN01"))
            .andExpect(jsonPath("$.ownerUserId").value(owner.id().toString()))
            .andExpect(jsonPath("$.ownerRoleCode").value("KEEPER"))
            .andExpect(jsonPath("$.updatedAt").isNotEmpty());

        mockMvc.perform(get("/api/inventory/items/{sku}/locations/{locationCode}", "arc-9250", "wh-item"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sku").value("ARC-9250"))
            .andExpect(jsonPath("$.locationCode").value("WH-ITEM"))
            .andExpect(jsonPath("$.onHandQuantity").value(14))
            .andExpect(jsonPath("$.availableQuantity").value(12))
            .andExpect(jsonPath("$.soldQuantity").value(2))
            .andExpect(jsonPath("$.unitOfMeasurementCode").value("CASE"))
            .andExpect(jsonPath("$.classificationCode").value("QUARANTINE"))
            .andExpect(jsonPath("$.productInstanceCode").value("PI-100"))
            .andExpect(jsonPath("$.externalReference").value("legacy-entry-100"))
            .andExpect(jsonPath("$.sourceSystemCode").value("LEGACY_INV"))
            .andExpect(jsonPath("$.ownerTenantCode").value("INVENTORYOWN01"))
            .andExpect(jsonPath("$.ownerUserId").value(owner.id().toString()))
            .andExpect(jsonPath("$.ownerRoleCode").value("KEEPER"));

        mockMvc.perform(get("/api/inventory/items")
            .param("sku", "ARC-9250")
            .param("classificationCode", "quarantine")
            .param("productInstanceCode", "pi-100")
            .param("externalReference", "legacy-entry-100")
            .param("sourceSystemCode", "legacy_inv")
            .param("ownerTenantCode", "inventoryown01")
            .param("ownerUserId", owner.id().toString())
            .param("ownerRoleCode", "keeper")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].sku").value("ARC-9250"))
            .andExpect(jsonPath("$.items[0].availableQuantity").value(12))
            .andExpect(jsonPath("$.items[0].soldQuantity").value(2))
            .andExpect(jsonPath("$.items[0].classificationCode").value("QUARANTINE"))
            .andExpect(jsonPath("$.items[0].productInstanceCode").value("PI-100"))
            .andExpect(jsonPath("$.items[0].externalReference").value("legacy-entry-100"))
            .andExpect(jsonPath("$.items[0].sourceSystemCode").value("LEGACY_INV"))
            .andExpect(jsonPath("$.items[0].ownerTenantCode").value("INVENTORYOWN01"))
            .andExpect(jsonPath("$.items[0].ownerUserId").value(owner.id().toString()))
            .andExpect(jsonPath("$.items[0].ownerRoleCode").value("KEEPER"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
            "/api/inventory/items/{sku}/locations/{locationCode}/metadata",
            "arc-9250",
            "wh-item"
        )
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {
                  "unitOfMeasurementCode": "each",
                  "classificationCode": "available",
                  "productInstanceCode": "pi-101",
                  "externalReference": " legacy-entry-101 ",
                  "sourceSystemCode": " warehouse_migration ",
                  "ownerTenantCode": " inventoryown01 ",
                  "ownerUserId": "%s",
                  "ownerRoleCode": " manager ",
                  "changedBy": " Inventory.Ops@ArcanaERP.com "
                }
                """.formatted(nextOwner.id())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sku").value("ARC-9250"))
            .andExpect(jsonPath("$.locationCode").value("WH-ITEM"))
            .andExpect(jsonPath("$.onHandQuantity").value(14))
            .andExpect(jsonPath("$.availableQuantity").value(12))
            .andExpect(jsonPath("$.soldQuantity").value(2))
            .andExpect(jsonPath("$.unitOfMeasurementCode").value("EACH"))
            .andExpect(jsonPath("$.classificationCode").value("AVAILABLE"))
            .andExpect(jsonPath("$.productInstanceCode").value("PI-101"))
            .andExpect(jsonPath("$.externalReference").value("legacy-entry-101"))
            .andExpect(jsonPath("$.sourceSystemCode").value("WAREHOUSE_MIGRATION"))
            .andExpect(jsonPath("$.ownerTenantCode").value("INVENTORYOWN01"))
            .andExpect(jsonPath("$.ownerUserId").value(nextOwner.id().toString()))
            .andExpect(jsonPath("$.ownerRoleCode").value("MANAGER"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
            "/api/inventory/items/{sku}/locations/{locationCode}/availability",
            "arc-9250",
            "wh-item"
        )
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {
                  "availableQuantityDelta": -3,
                  "soldQuantityDelta": 3,
                  "reason": "Sales allocation posted",
                  "changedBy": " Inventory.Ops@ArcanaERP.com "
                }
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sku").value("ARC-9250"))
            .andExpect(jsonPath("$.locationCode").value("WH-ITEM"))
            .andExpect(jsonPath("$.onHandQuantity").value(14))
            .andExpect(jsonPath("$.availableQuantity").value(9))
            .andExpect(jsonPath("$.soldQuantity").value(5))
            .andExpect(jsonPath("$.unitOfMeasurementCode").value("EACH"))
            .andExpect(jsonPath("$.classificationCode").value("AVAILABLE"))
            .andExpect(jsonPath("$.productInstanceCode").value("PI-101"))
            .andExpect(jsonPath("$.externalReference").value("legacy-entry-101"))
            .andExpect(jsonPath("$.sourceSystemCode").value("WAREHOUSE_MIGRATION"))
            .andExpect(jsonPath("$.ownerTenantCode").value("INVENTORYOWN01"))
            .andExpect(jsonPath("$.ownerUserId").value(nextOwner.id().toString()))
            .andExpect(jsonPath("$.ownerRoleCode").value("MANAGER"));

        mockMvc.perform(get("/api/inventory/items/{sku}/locations/{locationCode}/availability-history", "arc-9250", "wh-item")
            .param("changedBy", "inventory.ops@arcanaerp.com")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].sku").value("ARC-9250"))
            .andExpect(jsonPath("$.items[0].locationCode").value("WH-ITEM"))
            .andExpect(jsonPath("$.items[0].previousAvailableQuantity").value(12))
            .andExpect(jsonPath("$.items[0].currentAvailableQuantity").value(9))
            .andExpect(jsonPath("$.items[0].availableQuantityDelta").value(-3))
            .andExpect(jsonPath("$.items[0].previousSoldQuantity").value(2))
            .andExpect(jsonPath("$.items[0].currentSoldQuantity").value(5))
            .andExpect(jsonPath("$.items[0].soldQuantityDelta").value(3))
            .andExpect(jsonPath("$.items[0].reason").value("Sales allocation posted"))
            .andExpect(jsonPath("$.items[0].changedBy").value("inventory.ops@arcanaerp.com"))
            .andExpect(jsonPath("$.items[0].changedAt").isNotEmpty());

        mockMvc.perform(get("/api/inventory/items/{sku}/locations/{locationCode}/metadata-history", "arc-9250", "wh-item")
            .param("changedBy", "inventory.ops@arcanaerp.com")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].sku").value("ARC-9250"))
            .andExpect(jsonPath("$.items[0].locationCode").value("WH-ITEM"))
            .andExpect(jsonPath("$.items[0].previousUnitOfMeasurementCode").value("CASE"))
            .andExpect(jsonPath("$.items[0].currentUnitOfMeasurementCode").value("EACH"))
            .andExpect(jsonPath("$.items[0].previousClassificationCode").value("QUARANTINE"))
            .andExpect(jsonPath("$.items[0].currentClassificationCode").value("AVAILABLE"))
            .andExpect(jsonPath("$.items[0].previousProductInstanceCode").value("PI-100"))
            .andExpect(jsonPath("$.items[0].currentProductInstanceCode").value("PI-101"))
            .andExpect(jsonPath("$.items[0].previousExternalReference").value("legacy-entry-100"))
            .andExpect(jsonPath("$.items[0].currentExternalReference").value("legacy-entry-101"))
            .andExpect(jsonPath("$.items[0].previousSourceSystemCode").value("LEGACY_INV"))
            .andExpect(jsonPath("$.items[0].currentSourceSystemCode").value("WAREHOUSE_MIGRATION"))
            .andExpect(jsonPath("$.items[0].previousOwnerTenantCode").value("INVENTORYOWN01"))
            .andExpect(jsonPath("$.items[0].currentOwnerTenantCode").value("INVENTORYOWN01"))
            .andExpect(jsonPath("$.items[0].previousOwnerUserId").value(owner.id().toString()))
            .andExpect(jsonPath("$.items[0].currentOwnerUserId").value(nextOwner.id().toString()))
            .andExpect(jsonPath("$.items[0].previousOwnerRoleCode").value("KEEPER"))
            .andExpect(jsonPath("$.items[0].currentOwnerRoleCode").value("MANAGER"))
            .andExpect(jsonPath("$.items[0].changedBy").value("inventory.ops@arcanaerp.com"))
            .andExpect(jsonPath("$.items[0].changedAt").isNotEmpty());

        mockMvc.perform(get("/api/inventory/items/{sku}/locations/{locationCode}/owner-history", "arc-9250", "wh-item")
            .param("changedBy", "inventory.ops@arcanaerp.com")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].sku").value("ARC-9250"))
            .andExpect(jsonPath("$.items[0].locationCode").value("WH-ITEM"))
            .andExpect(jsonPath("$.items[0].previousOwnerTenantCode").value("INVENTORYOWN01"))
            .andExpect(jsonPath("$.items[0].currentOwnerTenantCode").value("INVENTORYOWN01"))
            .andExpect(jsonPath("$.items[0].previousOwnerUserId").value(owner.id().toString()))
            .andExpect(jsonPath("$.items[0].currentOwnerUserId").value(nextOwner.id().toString()))
            .andExpect(jsonPath("$.items[0].previousOwnerRoleCode").value("KEEPER"))
            .andExpect(jsonPath("$.items[0].currentOwnerRoleCode").value("MANAGER"))
            .andExpect(jsonPath("$.items[0].changedBy").value("inventory.ops@arcanaerp.com"))
            .andExpect(jsonPath("$.items[0].changedAt").isNotEmpty());
    }

    @Test
    void rejectsNoOpInventoryItemAvailabilityUpdate() throws Exception {
        inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9250a",
                "wh-availability-noop",
                new BigDecimal("5"),
                SEED_INSTANT
            )
        );

        expectBadRequest(
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
                "/api/inventory/items/{sku}/locations/{locationCode}/availability",
                "arc-9250a",
                "wh-availability-noop"
            )
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "availableQuantityDelta": 0,
                      "soldQuantityDelta": 0,
                      "reason": "No change",
                      "changedBy": "inventory.ops@arcanaerp.com"
                    }
                    """)),
            "Inventory item availability is unchanged",
            "/api/inventory/items/arc-9250a/locations/wh-availability-noop/availability"
        );
    }

    @Test
    void rejectsInventoryItemAvailabilityUpdateWhenAvailableQuantityWouldBecomeNegative() throws Exception {
        inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9250b",
                "wh-availability-negative",
                new BigDecimal("5"),
                SEED_INSTANT
            )
        );

        expectBadRequest(
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
                "/api/inventory/items/{sku}/locations/{locationCode}/availability",
                "arc-9250b",
                "wh-availability-negative"
            )
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "availableQuantityDelta": -6,
                      "soldQuantityDelta": 1,
                      "reason": "Oversold allocation",
                      "changedBy": "inventory.ops@arcanaerp.com"
                    }
                    """)),
            "availableQuantity cannot become negative",
            "/api/inventory/items/arc-9250b/locations/wh-availability-negative/availability"
        );
    }

    @Test
    void rejectsDuplicateInventoryItemSkuAndLocation() throws Exception {
        String payload = """
            {
              "sku": "arc-9251",
              "locationCode": "wh-dup",
              "onHandQuantity": 1
            }
            """;
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/inventory/items")
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content(payload))
            .andExpect(status().isCreated());

        expectConflict(
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/inventory/items")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(payload)),
            "Inventory item already exists for SKU/location: ARC-9251/WH-DUP",
            "/api/inventory/items"
        );
    }

    @Test
    void rejectsDuplicateInventoryItemExternalReferenceWithinSourceSystem() throws Exception {
        String payload = """
            {
              "sku": "arc-9251a",
              "locationCode": "wh-ext-ref-a",
              "onHandQuantity": 1,
              "externalReference": "legacy-entry-9251",
              "sourceSystemCode": "legacy_inv"
            }
            """;
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/inventory/items")
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content(payload))
            .andExpect(status().isCreated());

        expectConflict(
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/inventory/items")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "sku": "arc-9251b",
                      "locationCode": "wh-ext-ref-b",
                      "onHandQuantity": 1,
                      "externalReference": "legacy-entry-9251",
                      "sourceSystemCode": "legacy_inv"
                    }
                    """)),
            "Inventory item external reference already exists for source system: LEGACY_INV/legacy-entry-9251",
            "/api/inventory/items"
        );
    }

    @Test
    void rejectsInventoryItemRegistrationAtInactiveLocation() throws Exception {
        InventoryLocation inactiveLocation = inventoryLocationRepository.save(
            InventoryLocation.create("wh-inactive-item", "Inactive Item Warehouse", SEED_INSTANT)
        );
        inactiveLocation.setActive(false, SEED_INSTANT.plusSeconds(60));
        inventoryLocationRepository.save(inactiveLocation);

        expectBadRequest(
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/inventory/items")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "sku": "arc-9252",
                      "locationCode": "wh-inactive-item",
                      "onHandQuantity": 1
                    }
                    """)),
            "Inventory location is inactive: WH-INACTIVE-ITEM",
            "/api/inventory/items"
        );
    }

    @Test
    void rejectsInventoryItemRegistrationWithUnknownUnitOfMeasurement() throws Exception {
        expectBadRequest(
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/inventory/items")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "sku": "arc-9252a",
                      "locationCode": "wh-uom",
                      "onHandQuantity": 1,
                      "unitOfMeasurementCode": "crate"
                    }
                    """)),
            "Unit of measurement not found: CRATE",
            "/api/inventory/items"
        );
    }

    @Test
    void rejectsInventoryItemRegistrationWhenAvailableQuantityExceedsOnHandQuantity() throws Exception {
        expectBadRequest(
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/inventory/items")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "sku": "arc-9252b",
                      "locationCode": "wh-availability",
                      "onHandQuantity": 1,
                      "availableQuantity": 2
                    }
                    """)),
            "availableQuantity must not exceed onHandQuantity",
            "/api/inventory/items"
        );
    }

    @Test
    void rejectsInventoryItemRegistrationWithIncompleteOwnerMetadata() throws Exception {
        expectBadRequest(
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/inventory/items")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "sku": "arc-9252d",
                      "locationCode": "wh-owner",
                      "onHandQuantity": 1,
                      "ownerTenantCode": "inventoryown02"
                    }
                    """)),
            "ownerTenantCode, ownerUserId, and ownerRoleCode must be supplied together",
            "/api/inventory/items"
        );
    }

    @Test
    void filtersInventoryItemAvailabilityHistoryByChangedAtRange() throws Exception {
        InventoryItem item = inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9252c",
                "wh-availability-history",
                new BigDecimal("10"),
                new BigDecimal("8"),
                BigDecimal.ZERO,
                "EA",
                "ON_HAND",
                null,
                SEED_INSTANT
            )
        );
        availabilityChangeAuditRepository.save(InventoryItemAvailabilityChangeAudit.create(
            item.getId(),
            "arc-9252c",
            "wh-availability-history",
            new BigDecimal("8"),
            new BigDecimal("6"),
            new BigDecimal("-2"),
            BigDecimal.ZERO,
            new BigDecimal("2"),
            new BigDecimal("2"),
            "First allocation",
            "inventory.ops@arcanaerp.com",
            Instant.parse("2026-03-01T01:00:00Z")
        ));
        availabilityChangeAuditRepository.save(InventoryItemAvailabilityChangeAudit.create(
            item.getId(),
            "arc-9252c",
            "wh-availability-history",
            new BigDecimal("6"),
            new BigDecimal("4"),
            new BigDecimal("-2"),
            new BigDecimal("2"),
            new BigDecimal("4"),
            new BigDecimal("2"),
            "Second allocation",
            "planning@arcanaerp.com",
            Instant.parse("2026-03-02T01:00:00Z")
        ));

        mockMvc.perform(get(
            "/api/inventory/items/{sku}/locations/{locationCode}/availability-history",
            "arc-9252c",
            "wh-availability-history"
        )
            .param("changedAtFrom", "2026-03-02T00:00:00Z")
            .param("changedAtTo", "2026-03-02T23:59:59Z")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].currentAvailableQuantity").value(4))
            .andExpect(jsonPath("$.items[0].currentSoldQuantity").value(4))
            .andExpect(jsonPath("$.items[0].reason").value("Second allocation"))
            .andExpect(jsonPath("$.items[0].changedBy").value("planning@arcanaerp.com"));
    }

    @Test
    void rejectsNoOpInventoryItemMetadataUpdate() throws Exception {
        inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9253",
                "wh-noop",
                new BigDecimal("5"),
                "case",
                "quarantine",
                SEED_INSTANT
            )
        );

        expectBadRequest(
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
                "/api/inventory/items/{sku}/locations/{locationCode}/metadata",
                "arc-9253",
                "wh-noop"
            )
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "unitOfMeasurementCode": "case",
                      "classificationCode": "quarantine",
                      "changedBy": "inventory.ops@arcanaerp.com"
                    }
                    """)),
            "Inventory item metadata is unchanged",
            "/api/inventory/items/arc-9253/locations/wh-noop/metadata"
        );
    }

    @Test
    void rejectsInventoryItemMetadataUpdateWithUnknownUnitOfMeasurement() throws Exception {
        inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9253a",
                "wh-uom-update",
                new BigDecimal("5"),
                "case",
                "quarantine",
                SEED_INSTANT
            )
        );

        expectBadRequest(
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
                "/api/inventory/items/{sku}/locations/{locationCode}/metadata",
                "arc-9253a",
                "wh-uom-update"
            )
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "unitOfMeasurementCode": "crate",
                      "classificationCode": "available",
                      "changedBy": "inventory.ops@arcanaerp.com"
                    }
                    """)),
            "Unit of measurement not found: CRATE",
            "/api/inventory/items/arc-9253a/locations/wh-uom-update/metadata"
        );
    }

    @Test
    void rejectsInventoryItemMetadataUpdateWithDuplicateExternalReferenceWithinSourceSystem() throws Exception {
        inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9253b",
                "wh-ext-update-a",
                new BigDecimal("5"),
                "case",
                "quarantine",
                null,
                "legacy-entry-9253",
                "legacy_inv",
                SEED_INSTANT
            )
        );
        inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9253c",
                "wh-ext-update-b",
                new BigDecimal("5"),
                "case",
                "quarantine",
                SEED_INSTANT
            )
        );

        expectConflict(
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
                "/api/inventory/items/{sku}/locations/{locationCode}/metadata",
                "arc-9253c",
                "wh-ext-update-b"
            )
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "unitOfMeasurementCode": "case",
                      "classificationCode": "available",
                      "externalReference": "legacy-entry-9253",
                      "sourceSystemCode": "legacy_inv",
                      "changedBy": "inventory.ops@arcanaerp.com"
                    }
                    """)),
            "Inventory item external reference already exists for source system: LEGACY_INV/legacy-entry-9253",
            "/api/inventory/items/arc-9253c/locations/wh-ext-update-b/metadata"
        );
    }

    @Test
    void filtersInventoryItemMetadataHistoryByChangedAtRange() throws Exception {
        InventoryItem item = inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9254",
                "wh-history",
                new BigDecimal("5"),
                "case",
                "quarantine",
                SEED_INSTANT
            )
        );
        metadataChangeAuditRepository.save(InventoryItemMetadataChangeAudit.create(
            item.getId(),
            "arc-9254",
            "wh-history",
            "case",
            "each",
            "quarantine",
            "available",
            "pi-100",
            "pi-101",
            "inventory.ops@arcanaerp.com",
            Instant.parse("2026-03-01T01:00:00Z")
        ));
        metadataChangeAuditRepository.save(InventoryItemMetadataChangeAudit.create(
            item.getId(),
            "arc-9254",
            "wh-history",
            "each",
            "pallet",
            "available",
            "reserved",
            "pi-101",
            "pi-102",
            "planning@arcanaerp.com",
            Instant.parse("2026-03-02T01:00:00Z")
        ));

        mockMvc.perform(get("/api/inventory/items/{sku}/locations/{locationCode}/metadata-history", "arc-9254", "wh-history")
            .param("changedAtFrom", "2026-03-02T00:00:00Z")
            .param("changedAtTo", "2026-03-02T23:59:59Z")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].currentUnitOfMeasurementCode").value("PALLET"))
            .andExpect(jsonPath("$.items[0].currentClassificationCode").value("RESERVED"))
            .andExpect(jsonPath("$.items[0].currentProductInstanceCode").value("PI-102"))
            .andExpect(jsonPath("$.items[0].changedBy").value("planning@arcanaerp.com"));
    }

    @Test
    void rejectsAdjustmentAtInactiveInventoryLocation() throws Exception {
        InventoryLocation inactiveLocation = inventoryLocationRepository.save(
            InventoryLocation.create("wh-inactive", "Inactive Warehouse", SEED_INSTANT)
        );
        inactiveLocation.setActive(false, SEED_INSTANT.plusSeconds(60));
        inventoryLocationRepository.save(inactiveLocation);
        inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9202a",
                "wh-inactive",
                new BigDecimal("5"),
                SEED_INSTANT
            )
        );

        String payload = InventoryManagementWebTestSupport.adjustmentPayload(
            "1",
            "Receiving posted",
            DEFAULT_ACTOR
        );

        expectBadRequest(
            InventoryManagementWebTestSupport.adjustInventory(mockMvc, "arc-9202a", "wh-inactive", payload),
            "Inventory location is inactive: WH-INACTIVE",
            "/api/inventory/arc-9202a/adjustments"
        );
    }

    @Test
    void adjustsInventoryAtDefaultMainLocationAndAppendsAdjustmentHistory() throws Exception {
        InventoryItem item = inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9203",
                "main",
                new BigDecimal("10"),
                SEED_INSTANT
            )
        );

        String payload = InventoryManagementWebTestSupport.adjustmentPayload(
            "-3",
            "Cycle count correction",
            DEFAULT_ACTOR
        );

        InventoryManagementWebTestSupport.adjustInventory(mockMvc, "arc-9203", payload)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.sku").value("ARC-9203"))
            .andExpect(jsonPath("$.locationCode").value("MAIN"))
            .andExpect(jsonPath("$.previousOnHandQuantity").value(10))
            .andExpect(jsonPath("$.quantityDelta").value(-3))
            .andExpect(jsonPath("$.currentOnHandQuantity").value(7))
            .andExpect(jsonPath("$.reason").value("Cycle count correction"))
            .andExpect(jsonPath("$.adjustedBy").value(DEFAULT_ACTOR))
            .andExpect(jsonPath("$.adjustedAt").isNotEmpty());

        mockMvc.perform(get("/api/inventory/{sku}", "arc-9203"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.onHandQuantity").value(7));

        List<InventoryAdjustment> adjustments = inventoryAdjustmentRepository
            .findByInventoryItemIdOrderByAdjustedAtDesc(item.getId());
        assertThat(adjustments).hasSize(1);
        assertThat(adjustments.getFirst().getReason()).isEqualTo("Cycle count correction");
        assertThat(adjustments.getFirst().getAdjustedBy()).isEqualTo(DEFAULT_ACTOR);
        assertThat(adjustments.getFirst().getLocationCode()).isEqualTo("MAIN");
    }

    @Test
    void adjustsInventoryAtExplicitLocationWithoutAffectingMain() throws Exception {
        inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9204",
                "main",
                new BigDecimal("10"),
                SEED_INSTANT
            )
        );
        inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9204",
                "wh-west",
                new BigDecimal("4"),
                SEED_INSTANT
            )
        );

        String payload = InventoryManagementWebTestSupport.adjustmentPayload(
            "6",
            "Receiving posted",
            DEFAULT_ACTOR
        );

        InventoryManagementWebTestSupport.adjustInventory(mockMvc, "arc-9204", "wh-west", payload)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.locationCode").value("WH-WEST"))
            .andExpect(jsonPath("$.previousOnHandQuantity").value(4))
            .andExpect(jsonPath("$.currentOnHandQuantity").value(10));

        mockMvc.perform(get("/api/inventory/{sku}", "arc-9204"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.locationCode").value("MAIN"))
            .andExpect(jsonPath("$.onHandQuantity").value(10));

        mockMvc.perform(get("/api/inventory/{sku}", "arc-9204")
            .param("locationCode", "wh-west"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.locationCode").value("WH-WEST"))
            .andExpect(jsonPath("$.onHandQuantity").value(10));

        assertThat(inventoryLocationRepository.findByCode("WH-WEST")).isPresent();
    }

    @Test
    void readsDailyWeeklyAndMonthlyAdjustmentActivitySummaries() throws Exception {
        InventoryItem mainItem = inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9240",
                "main",
                new BigDecimal("20"),
                SEED_INSTANT
            )
        );
        InventoryItem westItem = inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9240",
                "wh-west",
                new BigDecimal("7"),
                SEED_INSTANT
            )
        );
        seedAdjustment(mainItem, "20", "-3", "17", "Cycle count correction", "ops-a@arcanaerp.com", "2027-02-01T01:00:00Z");
        seedAdjustment(mainItem, "17", "8", "25", "Receiving posted", "ops-b@arcanaerp.com", "2027-02-02T02:00:00Z");
        seedAdjustment(mainItem, "25", "-2", "23", "Pick variance", "ops-b@arcanaerp.com", "2027-02-08T03:00:00Z");
        seedAdjustment(westItem, "7", "5", "12", "West receiving", "ops-west@arcanaerp.com", "2027-02-02T04:00:00Z");

        mockMvc.perform(get("/api/inventory/{sku}/adjustment-activity/daily-summary", "arc-9240")
            .param("adjustedAtFrom", "2027-02-01T00:00:00Z")
            .param("adjustedAtTo", "2027-02-28T23:59:59Z")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(3))
            .andExpect(jsonPath("$.items[0].businessDate").value("2027-02-08"))
            .andExpect(jsonPath("$.items[0].netQuantityDelta").value(-2))
            .andExpect(jsonPath("$.items[1].businessDate").value("2027-02-02"))
            .andExpect(jsonPath("$.items[1].netQuantityDelta").value(8))
            .andExpect(jsonPath("$.items[2].businessDate").value("2027-02-01"))
            .andExpect(jsonPath("$.items[2].sku").value("ARC-9240"))
            .andExpect(jsonPath("$.items[2].locationCode").value("MAIN"));

        mockMvc.perform(get("/api/inventory/{sku}/adjustment-activity/weekly-summary", "arc-9240")
            .param("adjustedAtFrom", "2027-02-01T00:00:00Z")
            .param("adjustedAtTo", "2027-02-28T23:59:59Z")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.items[0].businessWeekStart").value("2027-02-08"))
            .andExpect(jsonPath("$.items[0].adjustmentCount").value(1))
            .andExpect(jsonPath("$.items[1].businessWeekStart").value("2027-02-01"))
            .andExpect(jsonPath("$.items[1].adjustmentCount").value(2))
            .andExpect(jsonPath("$.items[1].netQuantityDelta").value(5));

        mockMvc.perform(get("/api/inventory/{sku}/adjustment-activity/monthly-summary", "arc-9240")
            .param("adjustedBy", "OPS-B@ARCANAERP.COM")
            .param("adjustedAtFrom", "2027-02-01T00:00:00Z")
            .param("adjustedAtTo", "2027-02-28T23:59:59Z")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].businessMonth").value("2027-02"))
            .andExpect(jsonPath("$.items[0].adjustmentCount").value(2))
            .andExpect(jsonPath("$.items[0].netQuantityDelta").value(6));

        mockMvc.perform(get("/api/inventory/{sku}/adjustment-activity/daily-summary", "arc-9240")
            .param("locationCode", "wh-west")
            .param("adjustedAtFrom", "2027-02-01T00:00:00Z")
            .param("adjustedAtTo", "2027-02-28T23:59:59Z")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].locationCode").value("WH-WEST"))
            .andExpect(jsonPath("$.items[0].netQuantityDelta").value(5));

        mockMvc.perform(get("/api/inventory/{sku}/adjustment-activity/daily-summary", "arc-9240")
            .param("adjustedAtFrom", "2027-02-01T00:00:00Z")
            .param("adjustedAtTo", "2027-02-28T23:59:59Z")
            .param("page", "0")
            .param("size", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(3))
            .andExpect(jsonPath("$.items[0].businessDate").value("2027-02-08"))
            .andExpect(jsonPath("$.hasNext").value(true));
    }

    @Test
    void readsDailyWeeklyAndMonthlyAdjustmentActivitySummariesByLocation() throws Exception {
        InventoryItem mainItem = inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9241",
                "main",
                new BigDecimal("20"),
                SEED_INSTANT
            )
        );
        InventoryItem eastItem = inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9241",
                "wh-east",
                new BigDecimal("5"),
                SEED_INSTANT
            )
        );
        InventoryItem westItem = inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9241",
                "wh-west",
                new BigDecimal("7"),
                SEED_INSTANT
            )
        );
        seedAdjustment(mainItem, "20", "-3", "17", "Cycle count correction", "ops-a@arcanaerp.com", "2027-03-01T01:00:00Z");
        seedAdjustment(eastItem, "5", "6", "11", "East receiving", "ops-b@arcanaerp.com", "2027-03-01T02:00:00Z");
        seedAdjustment(westItem, "7", "4", "11", "West receiving", "ops-b@arcanaerp.com", "2027-03-08T03:00:00Z");

        mockMvc.perform(get("/api/inventory/{sku}/adjustment-activity/daily-summary/by-location", "arc-9241")
            .param("adjustedAtFrom", "2027-03-01T00:00:00Z")
            .param("adjustedAtTo", "2027-03-31T23:59:59Z")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(3))
            .andExpect(jsonPath("$.items[0].businessDate").value("2027-03-08"))
            .andExpect(jsonPath("$.items[0].locationCode").value("WH-WEST"))
            .andExpect(jsonPath("$.items[0].netQuantityDelta").value(4))
            .andExpect(jsonPath("$.items[1].businessDate").value("2027-03-01"))
            .andExpect(jsonPath("$.items[1].locationCode").value("MAIN"))
            .andExpect(jsonPath("$.items[2].businessDate").value("2027-03-01"))
            .andExpect(jsonPath("$.items[2].locationCode").value("WH-EAST"));

        mockMvc.perform(get("/api/inventory/{sku}/adjustment-activity/weekly-summary/by-location", "arc-9241")
            .param("adjustedAtFrom", "2027-03-01T00:00:00Z")
            .param("adjustedAtTo", "2027-03-31T23:59:59Z")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(3))
            .andExpect(jsonPath("$.items[0].businessWeekStart").value("2027-03-08"))
            .andExpect(jsonPath("$.items[0].locationCode").value("WH-WEST"))
            .andExpect(jsonPath("$.items[1].businessWeekStart").value("2027-03-01"))
            .andExpect(jsonPath("$.items[1].locationCode").value("MAIN"))
            .andExpect(jsonPath("$.items[2].locationCode").value("WH-EAST"));

        mockMvc.perform(get("/api/inventory/{sku}/adjustment-activity/monthly-summary/by-location", "arc-9241")
            .param("adjustedBy", "OPS-B@ARCANAERP.COM")
            .param("adjustedAtFrom", "2027-03-01T00:00:00Z")
            .param("adjustedAtTo", "2027-03-31T23:59:59Z")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.items[0].businessMonth").value("2027-03"))
            .andExpect(jsonPath("$.items[0].locationCode").value("WH-EAST"))
            .andExpect(jsonPath("$.items[0].netQuantityDelta").value(6))
            .andExpect(jsonPath("$.items[1].locationCode").value("WH-WEST"))
            .andExpect(jsonPath("$.items[1].netQuantityDelta").value(4));

        mockMvc.perform(get("/api/inventory/{sku}/adjustment-activity/daily-summary/by-location", "arc-9241")
            .param("locationCode", "wh-east")
            .param("adjustedAtFrom", "2027-03-01T00:00:00Z")
            .param("adjustedAtTo", "2027-03-31T23:59:59Z")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].locationCode").value("WH-EAST"));

        mockMvc.perform(get("/api/inventory/{sku}/adjustment-activity/daily-summary/by-location", "arc-9241")
            .param("adjustedAtFrom", "2027-03-01T00:00:00Z")
            .param("adjustedAtTo", "2027-03-31T23:59:59Z")
            .param("page", "0")
            .param("size", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(3))
            .andExpect(jsonPath("$.items[0].businessDate").value("2027-03-08"))
            .andExpect(jsonPath("$.hasNext").value(true));
    }

    @Test
    void readsDailyWeeklyAndMonthlyAdjustmentActivitySummariesByAdjustedBy() throws Exception {
        InventoryItem mainItem = inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9242",
                "main",
                new BigDecimal("20"),
                SEED_INSTANT
            )
        );
        InventoryItem eastItem = inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9242",
                "wh-east",
                new BigDecimal("5"),
                SEED_INSTANT
            )
        );
        InventoryItem westItem = inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9242",
                "wh-west",
                new BigDecimal("7"),
                SEED_INSTANT
            )
        );
        seedAdjustment(mainItem, "20", "-3", "17", "Cycle count correction", "ops-a@arcanaerp.com", "2027-04-05T01:00:00Z");
        seedAdjustment(eastItem, "5", "6", "11", "East receiving", "ops-b@arcanaerp.com", "2027-04-05T02:00:00Z");
        seedAdjustment(westItem, "7", "4", "11", "West receiving", "ops-b@arcanaerp.com", "2027-04-12T03:00:00Z");
        seedAdjustment(mainItem, "17", "-2", "15", "Pick variance", "ops-c@arcanaerp.com", "2027-04-12T04:00:00Z");

        mockMvc.perform(get("/api/inventory/{sku}/adjustment-activity/daily-summary/by-adjusted-by", "arc-9242")
            .param("adjustedAtFrom", "2027-04-01T00:00:00Z")
            .param("adjustedAtTo", "2027-04-30T23:59:59Z")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(4))
            .andExpect(jsonPath("$.items[0].businessDate").value("2027-04-12"))
            .andExpect(jsonPath("$.items[0].adjustedBy").value("ops-b@arcanaerp.com"))
            .andExpect(jsonPath("$.items[0].netQuantityDelta").value(4))
            .andExpect(jsonPath("$.items[1].businessDate").value("2027-04-12"))
            .andExpect(jsonPath("$.items[1].adjustedBy").value("ops-c@arcanaerp.com"))
            .andExpect(jsonPath("$.items[2].businessDate").value("2027-04-05"))
            .andExpect(jsonPath("$.items[2].adjustedBy").value("ops-a@arcanaerp.com"))
            .andExpect(jsonPath("$.items[3].businessDate").value("2027-04-05"))
            .andExpect(jsonPath("$.items[3].adjustedBy").value("ops-b@arcanaerp.com"))
            .andExpect(jsonPath("$.items[3].sku").value("ARC-9242"));

        mockMvc.perform(get("/api/inventory/{sku}/adjustment-activity/weekly-summary/by-adjusted-by", "arc-9242")
            .param("adjustedAtFrom", "2027-04-01T00:00:00Z")
            .param("adjustedAtTo", "2027-04-30T23:59:59Z")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(4))
            .andExpect(jsonPath("$.items[0].businessWeekStart").value("2027-04-12"))
            .andExpect(jsonPath("$.items[0].adjustedBy").value("ops-b@arcanaerp.com"))
            .andExpect(jsonPath("$.items[1].businessWeekStart").value("2027-04-12"))
            .andExpect(jsonPath("$.items[1].adjustedBy").value("ops-c@arcanaerp.com"))
            .andExpect(jsonPath("$.items[2].businessWeekStart").value("2027-04-05"))
            .andExpect(jsonPath("$.items[2].adjustedBy").value("ops-a@arcanaerp.com"))
            .andExpect(jsonPath("$.items[3].adjustedBy").value("ops-b@arcanaerp.com"));

        mockMvc.perform(get("/api/inventory/{sku}/adjustment-activity/monthly-summary/by-adjusted-by", "arc-9242")
            .param("adjustedBy", "OPS-B@ARCANAERP.COM")
            .param("adjustedAtFrom", "2027-04-01T00:00:00Z")
            .param("adjustedAtTo", "2027-04-30T23:59:59Z")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].businessMonth").value("2027-04"))
            .andExpect(jsonPath("$.items[0].adjustedBy").value("ops-b@arcanaerp.com"))
            .andExpect(jsonPath("$.items[0].adjustmentCount").value(2))
            .andExpect(jsonPath("$.items[0].netQuantityDelta").value(10));

        mockMvc.perform(get("/api/inventory/{sku}/adjustment-activity/monthly-summary/by-adjusted-by", "arc-9242")
            .param("locationCode", "main")
            .param("adjustedAtFrom", "2027-04-01T00:00:00Z")
            .param("adjustedAtTo", "2027-04-30T23:59:59Z")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.items[0].adjustedBy").value("ops-a@arcanaerp.com"))
            .andExpect(jsonPath("$.items[0].netQuantityDelta").value(-3))
            .andExpect(jsonPath("$.items[1].adjustedBy").value("ops-c@arcanaerp.com"))
            .andExpect(jsonPath("$.items[1].netQuantityDelta").value(-2));

        mockMvc.perform(get("/api/inventory/{sku}/adjustment-activity/daily-summary/by-adjusted-by", "arc-9242")
            .param("adjustedAtFrom", "2027-04-01T00:00:00Z")
            .param("adjustedAtTo", "2027-04-30T23:59:59Z")
            .param("page", "0")
            .param("size", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(4))
            .andExpect(jsonPath("$.items[0].businessDate").value("2027-04-12"))
            .andExpect(jsonPath("$.items[0].adjustedBy").value("ops-b@arcanaerp.com"))
            .andExpect(jsonPath("$.hasNext").value(true));
    }

    @Test
    void transfersInventoryBetweenLocationsWithPairedAdjustmentRecords() throws Exception {
        inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9207",
                "main",
                new BigDecimal("12"),
                SEED_INSTANT
            )
        );
        inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9207",
                "wh-east",
                new BigDecimal("3"),
                SEED_INSTANT
            )
        );

        String payload = InventoryManagementWebTestSupport.transferPayload(
            "main",
            "wh-east",
            "5",
            "Rebalancing transfer",
            DEFAULT_ACTOR,
            "fulfillment",
            "FUL-9207-1"
        );

        InventoryManagementWebTestSupport.transferInventory(mockMvc, "arc-9207", payload)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.transferId").isNotEmpty())
            .andExpect(jsonPath("$.sku").value("ARC-9207"))
            .andExpect(jsonPath("$.sourceLocationCode").value("MAIN"))
            .andExpect(jsonPath("$.destinationLocationCode").value("WH-EAST"))
            .andExpect(jsonPath("$.quantity").value(5))
            .andExpect(jsonPath("$.sourceOnHandQuantity").value(7))
            .andExpect(jsonPath("$.destinationOnHandQuantity").value(8))
            .andExpect(jsonPath("$.reason").value("Rebalancing transfer"))
            .andExpect(jsonPath("$.adjustedBy").value(DEFAULT_ACTOR))
            .andExpect(jsonPath("$.referenceType").value("FULFILLMENT"))
            .andExpect(jsonPath("$.referenceId").value("FUL-9207-1"))
            .andExpect(jsonPath("$.transferredAt").isNotEmpty());

        mockMvc.perform(get("/api/inventory/{sku}", "arc-9207"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.locationCode").value("MAIN"))
            .andExpect(jsonPath("$.onHandQuantity").value(7));

        mockMvc.perform(get("/api/inventory/{sku}", "arc-9207")
            .param("locationCode", "wh-east"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.locationCode").value("WH-EAST"))
            .andExpect(jsonPath("$.onHandQuantity").value(8));

        InventoryItem sourceItem = inventoryItemRepository.findBySkuAndLocationCode("ARC-9207", "MAIN").orElseThrow();
        InventoryItem destinationItem = inventoryItemRepository.findBySkuAndLocationCode("ARC-9207", "WH-EAST").orElseThrow();
        InventoryAdjustment sourceAdjustment = inventoryAdjustmentRepository
            .findByInventoryItemIdOrderByAdjustedAtDesc(sourceItem.getId())
            .getFirst();
        InventoryAdjustment destinationAdjustment = inventoryAdjustmentRepository
            .findByInventoryItemIdOrderByAdjustedAtDesc(destinationItem.getId())
            .getFirst();

        assertThat(sourceAdjustment.getTransferId()).isNotNull();
        assertThat(destinationAdjustment.getTransferId()).isEqualTo(sourceAdjustment.getTransferId());
        assertThat(sourceAdjustment.getQuantityDelta()).isEqualByComparingTo("-5");
        assertThat(destinationAdjustment.getQuantityDelta()).isEqualByComparingTo("5");
        assertThat(sourceAdjustment.getReferenceType()).isEqualTo("FULFILLMENT");
        assertThat(sourceAdjustment.getReferenceId()).isEqualTo("FUL-9207-1");
        assertThat(destinationAdjustment.getReferenceType()).isEqualTo("FULFILLMENT");
        assertThat(destinationAdjustment.getReferenceId()).isEqualTo("FUL-9207-1");

        List<InventoryAdjustment> transferAdjustments = inventoryAdjustmentRepository
            .findByTransferIdOrderByAdjustedAtAsc(sourceAdjustment.getTransferId());
        assertThat(transferAdjustments).hasSize(2);
    }

    @Test
    void recordsPickupAndDropoffInventoryTransactionsWithAdjustmentLinks() throws Exception {
        inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9250",
                "main",
                new BigDecimal("12"),
                SEED_INSTANT
            )
        );
        inventoryFacilityRepository.save(InventoryFacility.create(
            "fulfill-west",
            "Fulfillment West",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            SEED_INSTANT
        ));
        inventoryFixedAssetRepository.save(InventoryFixedAsset.create(
            "truck-7",
            "Truck 7",
            "vehicle",
            null,
            null,
            null,
            SEED_INSTANT
        ));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/inventory/{sku}/pickup-dropoffs", "arc-9250")
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {
                  "locationCode": " main ",
                  "transactionTypeCode": " pickup ",
                  "quantity": 3,
                  "reason": "Customer pickup",
                  "handledBy": "OPS@ARCANAERP.COM",
                  "fixedAssetCode": " truck-7 ",
                  "facilityCode": " fulfill-west ",
                  "referenceType": "shipment",
                  "referenceId": "SHP-9250-1"
                }
                """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.inventoryAdjustmentId").isNotEmpty())
            .andExpect(jsonPath("$.sku").value("ARC-9250"))
            .andExpect(jsonPath("$.locationCode").value("MAIN"))
            .andExpect(jsonPath("$.transactionTypeCode").value("PICKUP"))
            .andExpect(jsonPath("$.quantity").value(3))
            .andExpect(jsonPath("$.quantityDelta").value(-3))
            .andExpect(jsonPath("$.previousOnHandQuantity").value(12))
            .andExpect(jsonPath("$.currentOnHandQuantity").value(9))
            .andExpect(jsonPath("$.reason").value("Customer pickup"))
            .andExpect(jsonPath("$.handledBy").value("ops@arcanaerp.com"))
            .andExpect(jsonPath("$.fixedAssetCode").value("TRUCK-7"))
            .andExpect(jsonPath("$.facilityCode").value("FULFILL-WEST"))
            .andExpect(jsonPath("$.referenceType").value("SHIPMENT"))
            .andExpect(jsonPath("$.referenceId").value("SHP-9250-1"))
            .andExpect(jsonPath("$.transactionAt").isNotEmpty());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/inventory/{sku}/pickup-dropoffs", "arc-9250")
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {
                  "locationCode": "main",
                  "transactionTypeCode": "dropoff",
                  "quantity": 2,
                  "reason": "Returned at dock",
                  "handledBy": "receiving@arcanaerp.com"
                }
                """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.transactionTypeCode").value("DROPOFF"))
            .andExpect(jsonPath("$.quantity").value(2))
            .andExpect(jsonPath("$.quantityDelta").value(2))
            .andExpect(jsonPath("$.previousOnHandQuantity").value(9))
            .andExpect(jsonPath("$.currentOnHandQuantity").value(11))
            .andExpect(jsonPath("$.handledBy").value("receiving@arcanaerp.com"));

        mockMvc.perform(get("/api/inventory/{sku}", "arc-9250"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.onHandQuantity").value(11));

        InventoryItem item = inventoryItemRepository.findBySkuAndLocationCode("ARC-9250", "MAIN").orElseThrow();
        List<InventoryAdjustment> adjustments = inventoryAdjustmentRepository.findByInventoryItemIdOrderByAdjustedAtDesc(item.getId());
        assertThat(adjustments).hasSize(2);
        assertThat(adjustments.get(0).getQuantityDelta()).isEqualByComparingTo("2");
        assertThat(adjustments.get(0).getReferenceType()).isEqualTo("DROPOFF");
        assertThat(adjustments.get(1).getQuantityDelta()).isEqualByComparingTo("-3");
        assertThat(adjustments.get(1).getReferenceType()).isEqualTo("PICKUP");

        List<InventoryPickupDropoffTransaction> transactions = pickupDropoffTransactionRepository.findAll();
        assertThat(transactions).hasSize(2);
        assertThat(transactions)
            .extracting(InventoryPickupDropoffTransaction::getInventoryAdjustmentId)
            .containsExactlyInAnyOrderElementsOf(adjustments.stream().map(InventoryAdjustment::getId).toList());
    }

    @Test
    void listsPickupDropoffInventoryTransactionsWithFilters() throws Exception {
        inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9251",
                "main",
                new BigDecimal("20"),
                SEED_INSTANT
            )
        );
        inventoryFacilityRepository.save(InventoryFacility.create(
            "dock-west",
            "Dock West",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            SEED_INSTANT
        ));
        inventoryFixedAssetRepository.save(InventoryFixedAsset.create(
            "forklift-11",
            "Forklift 11",
            "equipment",
            null,
            null,
            null,
            SEED_INSTANT
        ));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/inventory/{sku}/pickup-dropoffs", "arc-9251")
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {
                  "locationCode": "main",
                  "transactionTypeCode": "pickup",
                  "quantity": 4,
                  "reason": "Shipment load",
                  "handledBy": "dock-a@arcanaerp.com",
                  "fixedAssetCode": "forklift-11",
                  "facilityCode": "dock-west",
                  "referenceType": "shipment",
                  "referenceId": "SHP-9251-1"
                }
                """))
            .andExpect(status().isCreated());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/inventory/{sku}/pickup-dropoffs", "arc-9251")
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {
                  "locationCode": "main",
                  "transactionTypeCode": "dropoff",
                  "quantity": 1,
                  "reason": "Return dock",
                  "handledBy": "dock-b@arcanaerp.com",
                  "referenceType": "rma",
                  "referenceId": "RMA-9251-1"
                }
                """))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/inventory/{sku}/pickup-dropoffs", "arc-9251")
            .param("transactionTypeCode", "pickup")
            .param("handledBy", " DOCK-A@ARCANAERP.COM ")
            .param("fixedAssetCode", " FORKLIFT-11 ")
            .param("facilityCode", " DOCK-WEST ")
            .param("referenceType", "shipment")
            .param("referenceId", "SHP-9251-1")
            .param("transactionAtFrom", "2026-01-01T00:00:00Z")
            .param("transactionAtTo", "2030-01-01T00:00:00Z")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].transactionTypeCode").value("PICKUP"))
            .andExpect(jsonPath("$.items[0].handledBy").value("dock-a@arcanaerp.com"))
            .andExpect(jsonPath("$.items[0].fixedAssetCode").value("FORKLIFT-11"))
            .andExpect(jsonPath("$.items[0].facilityCode").value("DOCK-WEST"))
            .andExpect(jsonPath("$.items[0].referenceType").value("SHIPMENT"))
            .andExpect(jsonPath("$.items[0].referenceId").value("SHP-9251-1"))
            .andExpect(jsonPath("$.items[0].quantityDelta").value(-4));

        mockMvc.perform(get("/api/inventory/{sku}/pickup-dropoffs", "arc-9251")
            .param("transactionTypeCode", "dropoff")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].transactionTypeCode").value("DROPOFF"))
            .andExpect(jsonPath("$.items[0].quantityDelta").value(1));
    }

    @Test
    void readsDailyWeeklyAndMonthlyPickupDropoffActivitySummaries() throws Exception {
        inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9251a",
                "main",
                new BigDecimal("30"),
                SEED_INSTANT
            )
        );
        inventoryFacilityRepository.save(InventoryFacility.create(
            "dock-east",
            "Dock East",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            SEED_INSTANT
        ));
        inventoryFixedAssetRepository.save(InventoryFixedAsset.create(
            "trailer-12",
            "Trailer 12",
            "trailer",
            null,
            null,
            null,
            SEED_INSTANT
        ));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/inventory/{sku}/pickup-dropoffs", "arc-9251a")
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {
                  "locationCode": "main",
                  "transactionTypeCode": "pickup",
                  "quantity": 4,
                  "reason": "First load",
                  "handledBy": "dock-a@arcanaerp.com",
                  "fixedAssetCode": "trailer-12",
                  "facilityCode": "dock-east",
                  "referenceType": "shipment",
                  "referenceId": "SHP-9251A-1"
                }
                """))
            .andExpect(status().isCreated());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/inventory/{sku}/pickup-dropoffs", "arc-9251a")
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {
                  "locationCode": "main",
                  "transactionTypeCode": "pickup",
                  "quantity": 3,
                  "reason": "Second load",
                  "handledBy": "dock-a@arcanaerp.com",
                  "fixedAssetCode": "trailer-12",
                  "facilityCode": "dock-east",
                  "referenceType": "shipment",
                  "referenceId": "SHP-9251A-1"
                }
                """))
            .andExpect(status().isCreated());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/inventory/{sku}/pickup-dropoffs", "arc-9251a")
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {
                  "locationCode": "main",
                  "transactionTypeCode": "dropoff",
                  "quantity": 2,
                  "reason": "Dock return",
                  "handledBy": "dock-b@arcanaerp.com",
                  "referenceType": "rma",
                  "referenceId": "RMA-9251A-1"
                }
                """))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/inventory/{sku}/pickup-dropoff-activity/daily-summary", "arc-9251a")
            .param("locationCode", "main")
            .param("transactionTypeCode", "pickup")
            .param("handledBy", "DOCK-A@ARCANAERP.COM")
            .param("fixedAssetCode", "trailer-12")
            .param("facilityCode", "dock-east")
            .param("referenceType", "shipment")
            .param("referenceId", "SHP-9251A-1")
            .param("transactionAtFrom", "2026-01-01T00:00:00Z")
            .param("transactionAtTo", "2030-01-01T00:00:00Z")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].sku").value("ARC-9251A"))
            .andExpect(jsonPath("$.items[0].businessDate").isNotEmpty())
            .andExpect(jsonPath("$.items[0].locationCode").value("MAIN"))
            .andExpect(jsonPath("$.items[0].transactionTypeCode").value("PICKUP"))
            .andExpect(jsonPath("$.items[0].handledBy").value("dock-a@arcanaerp.com"))
            .andExpect(jsonPath("$.items[0].transactionCount").value(2))
            .andExpect(jsonPath("$.items[0].totalPickupQuantity").value(7))
            .andExpect(jsonPath("$.items[0].totalDropoffQuantity").value(0))
            .andExpect(jsonPath("$.items[0].netQuantityDelta").value(-7));

        mockMvc.perform(get("/api/inventory/{sku}/pickup-dropoff-activity/weekly-summary", "arc-9251a")
            .param("transactionAtFrom", "2026-01-01T00:00:00Z")
            .param("transactionAtTo", "2030-01-01T00:00:00Z")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.items[0].businessWeekStart").isNotEmpty())
            .andExpect(jsonPath("$.items[0].locationCode").value("MAIN"))
            .andExpect(jsonPath("$.items[0].transactionTypeCode").value("DROPOFF"))
            .andExpect(jsonPath("$.items[0].totalDropoffQuantity").value(2))
            .andExpect(jsonPath("$.items[0].netQuantityDelta").value(2))
            .andExpect(jsonPath("$.items[1].transactionTypeCode").value("PICKUP"))
            .andExpect(jsonPath("$.items[1].totalPickupQuantity").value(7))
            .andExpect(jsonPath("$.items[1].netQuantityDelta").value(-7));

        mockMvc.perform(get("/api/inventory/{sku}/pickup-dropoff-activity/monthly-summary", "arc-9251a")
            .param("transactionTypeCode", "dropoff")
            .param("transactionAtFrom", "2026-01-01T00:00:00Z")
            .param("transactionAtTo", "2030-01-01T00:00:00Z")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].businessMonth").isNotEmpty())
            .andExpect(jsonPath("$.items[0].transactionTypeCode").value("DROPOFF"))
            .andExpect(jsonPath("$.items[0].transactionCount").value(1))
            .andExpect(jsonPath("$.items[0].totalPickupQuantity").value(0))
            .andExpect(jsonPath("$.items[0].totalDropoffQuantity").value(2))
            .andExpect(jsonPath("$.items[0].netQuantityDelta").value(2));
    }

    @Test
    void rejectsPickupDropoffActivitySummaryWhenTransactionAtRangeInvalid() throws Exception {
        inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9251b",
                "main",
                new BigDecimal("5"),
                SEED_INSTANT
            )
        );

        expectBadRequest(
            mockMvc.perform(get("/api/inventory/{sku}/pickup-dropoff-activity/daily-summary", "arc-9251b")
                .param("transactionAtFrom", "2030-01-01T00:00:00Z")
                .param("transactionAtTo", "2026-01-01T00:00:00Z")),
            "transactionAtFrom must be before or equal to transactionAtTo",
            "/api/inventory/arc-9251b/pickup-dropoff-activity/daily-summary"
        );
    }

    @Test
    void rejectsInvalidPickupDropoffInventoryTransactions() throws Exception {
        inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9252",
                "main",
                new BigDecimal("2"),
                SEED_INSTANT
            )
        );

        expectBadRequest(
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/inventory/{sku}/pickup-dropoffs", "arc-9252")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "locationCode": "main",
                      "transactionTypeCode": "move",
                      "quantity": 1,
                      "reason": "Bad type",
                      "handledBy": "ops@arcanaerp.com"
                    }
                    """)),
            "transactionTypeCode must be PICKUP or DROPOFF",
            "/api/inventory/arc-9252/pickup-dropoffs"
        );

        expectBadRequest(
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/inventory/{sku}/pickup-dropoffs", "arc-9252")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "locationCode": "main",
                      "transactionTypeCode": "pickup",
                      "quantity": 0,
                      "reason": "Bad quantity",
                      "handledBy": "ops@arcanaerp.com"
                    }
                    """)),
            "quantity must be greater than zero",
            "/api/inventory/arc-9252/pickup-dropoffs"
        );

        expectBadRequest(
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/inventory/{sku}/pickup-dropoffs", "arc-9252")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "locationCode": "main",
                      "transactionTypeCode": "pickup",
                      "quantity": 1,
                      "reason": "Missing reference id",
                      "handledBy": "ops@arcanaerp.com",
                      "referenceType": "shipment"
                    }
                    """)),
            "referenceType and referenceId must both be provided together",
            "/api/inventory/arc-9252/pickup-dropoffs"
        );

        expectInventoryFacilityNotFound(
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/inventory/{sku}/pickup-dropoffs", "arc-9252")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "locationCode": "main",
                      "transactionTypeCode": "pickup",
                      "quantity": 1,
                      "reason": "Unknown facility",
                      "handledBy": "ops@arcanaerp.com",
                      "facilityCode": "missing-facility"
                    }
                    """)),
            "MISSING-FACILITY",
            "/api/inventory/arc-9252/pickup-dropoffs"
        );

        expectInventoryFixedAssetNotFound(
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/inventory/{sku}/pickup-dropoffs", "arc-9252")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "locationCode": "main",
                      "transactionTypeCode": "pickup",
                      "quantity": 1,
                      "reason": "Unknown fixed asset",
                      "handledBy": "ops@arcanaerp.com",
                      "fixedAssetCode": "missing-asset"
                    }
                    """)),
            "MISSING-ASSET",
            "/api/inventory/arc-9252/pickup-dropoffs"
        );
    }

    @Test
    void rejectsPickupThatWouldMakeInventoryNegative() throws Exception {
        inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9253",
                "main",
                new BigDecimal("2"),
                SEED_INSTANT
            )
        );

        expectBadRequest(
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/inventory/{sku}/pickup-dropoffs", "arc-9253")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "locationCode": "main",
                      "transactionTypeCode": "pickup",
                      "quantity": 3,
                      "reason": "Over pickup",
                      "handledBy": "ops@arcanaerp.com"
                    }
                    """)),
            "onHandQuantity cannot become negative",
            "/api/inventory/arc-9253/pickup-dropoffs"
        );
    }

    @Test
    void transfersInventoryAndCreatesDestinationLocationStockWhenMissing() throws Exception {
        inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9208",
                "main",
                new BigDecimal("9"),
                SEED_INSTANT
            )
        );

        String payload = InventoryManagementWebTestSupport.transferPayload(
            "main",
            "wh-north",
            "2",
            "Initial stocking transfer",
            DEFAULT_ACTOR
        );

        InventoryManagementWebTestSupport.transferInventory(mockMvc, "arc-9208", payload)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.sourceOnHandQuantity").value(7))
            .andExpect(jsonPath("$.destinationOnHandQuantity").value(2));

        mockMvc.perform(get("/api/inventory/{sku}", "arc-9208")
            .param("locationCode", "wh-north"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.locationCode").value("WH-NORTH"))
            .andExpect(jsonPath("$.onHandQuantity").value(2))
            .andExpect(jsonPath("$.unitOfMeasurementCode").value("EA"))
            .andExpect(jsonPath("$.classificationCode").value("ON_HAND"));

        assertThat(inventoryLocationRepository.findByCode("WH-NORTH")).isPresent();
    }

    @Test
    void transferCreatedDestinationStockCopiesSourceMetadata() throws Exception {
        inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9208a",
                "main",
                new BigDecimal("9"),
                "pallet",
                "available",
                SEED_INSTANT
            )
        );

        String payload = InventoryManagementWebTestSupport.transferPayload(
            "main",
            "wh-south",
            "2",
            "Initial stocking transfer",
            DEFAULT_ACTOR
        );

        InventoryManagementWebTestSupport.transferInventory(mockMvc, "arc-9208a", payload)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.sourceOnHandQuantity").value(7))
            .andExpect(jsonPath("$.destinationOnHandQuantity").value(2));

        mockMvc.perform(get("/api/inventory/{sku}", "arc-9208a")
            .param("locationCode", "wh-south"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.locationCode").value("WH-SOUTH"))
            .andExpect(jsonPath("$.onHandQuantity").value(2))
            .andExpect(jsonPath("$.unitOfMeasurementCode").value("PALLET"))
            .andExpect(jsonPath("$.classificationCode").value("AVAILABLE"));
    }

    @Test
    void readsDailyWeeklyAndMonthlyTransferActivitySummaries() throws Exception {
        InventoryItem mainItem = inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9243",
                "main",
                new BigDecimal("20"),
                SEED_INSTANT
            )
        );
        InventoryItem eastItem = inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9243",
                "wh-east",
                new BigDecimal("5"),
                SEED_INSTANT
            )
        );
        InventoryItem westItem = inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9243",
                "wh-west",
                new BigDecimal("7"),
                SEED_INSTANT
            )
        );
        seedTransfer(mainItem, eastItem, "20", "17", "5", "8", "3", "Main to east", "ops-a@arcanaerp.com", "FULFILLMENT", "FUL-9243-1", "2027-05-03T01:00:00Z");
        seedTransfer(mainItem, eastItem, "17", "15", "8", "10", "2", "Main to east repeat", "ops-a@arcanaerp.com", "FULFILLMENT", "FUL-9243-2", "2027-05-03T02:00:00Z");
        seedTransfer(eastItem, westItem, "10", "6", "7", "11", "4", "East to west", "ops-b@arcanaerp.com", "REBALANCE", "REB-9243-1", "2027-05-10T03:00:00Z");
        seedTransfer(westItem, mainItem, "11", "5", "15", "21", "6", "West to main", "ops-c@arcanaerp.com", "REBALANCE", "REB-9243-2", "2027-05-10T04:00:00Z");

        mockMvc.perform(get("/api/inventory/{sku}/transfer-activity/daily-summary", "arc-9243")
            .param("adjustedAtFrom", "2027-05-01T00:00:00Z")
            .param("adjustedAtTo", "2027-05-31T23:59:59Z")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(3))
            .andExpect(jsonPath("$.items[0].businessDate").value("2027-05-10"))
            .andExpect(jsonPath("$.items[0].sourceLocationCode").value("WH-EAST"))
            .andExpect(jsonPath("$.items[0].destinationLocationCode").value("WH-WEST"))
            .andExpect(jsonPath("$.items[0].adjustedBy").value("ops-b@arcanaerp.com"))
            .andExpect(jsonPath("$.items[0].transferCount").value(1))
            .andExpect(jsonPath("$.items[0].totalQuantity").value(4))
            .andExpect(jsonPath("$.items[1].sourceLocationCode").value("WH-WEST"))
            .andExpect(jsonPath("$.items[1].destinationLocationCode").value("MAIN"))
            .andExpect(jsonPath("$.items[2].businessDate").value("2027-05-03"))
            .andExpect(jsonPath("$.items[2].sourceLocationCode").value("MAIN"))
            .andExpect(jsonPath("$.items[2].destinationLocationCode").value("WH-EAST"))
            .andExpect(jsonPath("$.items[2].transferCount").value(2))
            .andExpect(jsonPath("$.items[2].totalQuantity").value(5));

        mockMvc.perform(get("/api/inventory/{sku}/transfer-activity/weekly-summary", "arc-9243")
            .param("adjustedAtFrom", "2027-05-01T00:00:00Z")
            .param("adjustedAtTo", "2027-05-31T23:59:59Z")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(3))
            .andExpect(jsonPath("$.items[0].businessWeekStart").value("2027-05-10"))
            .andExpect(jsonPath("$.items[1].businessWeekStart").value("2027-05-10"))
            .andExpect(jsonPath("$.items[2].businessWeekStart").value("2027-05-03"))
            .andExpect(jsonPath("$.items[2].transferCount").value(2))
            .andExpect(jsonPath("$.items[2].totalQuantity").value(5));

        mockMvc.perform(get("/api/inventory/{sku}/transfer-activity/monthly-summary", "arc-9243")
            .param("sourceLocationCode", "main")
            .param("destinationLocationCode", "wh-east")
            .param("adjustedBy", "OPS-A@ARCANAERP.COM")
            .param("referenceType", "fulfillment")
            .param("adjustedAtFrom", "2027-05-01T00:00:00Z")
            .param("adjustedAtTo", "2027-05-31T23:59:59Z")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].businessMonth").value("2027-05"))
            .andExpect(jsonPath("$.items[0].sourceLocationCode").value("MAIN"))
            .andExpect(jsonPath("$.items[0].destinationLocationCode").value("WH-EAST"))
            .andExpect(jsonPath("$.items[0].adjustedBy").value("ops-a@arcanaerp.com"))
            .andExpect(jsonPath("$.items[0].transferCount").value(2))
            .andExpect(jsonPath("$.items[0].totalQuantity").value(5));

        mockMvc.perform(get("/api/inventory/{sku}/transfer-activity/daily-summary", "arc-9243")
            .param("adjustedAtFrom", "2027-05-01T00:00:00Z")
            .param("adjustedAtTo", "2027-05-31T23:59:59Z")
            .param("page", "0")
            .param("size", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(3))
            .andExpect(jsonPath("$.items[0].businessDate").value("2027-05-10"))
            .andExpect(jsonPath("$.hasNext").value(true));
    }

    @Test
    void readsDailyWeeklyAndMonthlyTransferActivitySummariesByReference() throws Exception {
        InventoryItem mainItem = inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9244",
                "main",
                new BigDecimal("20"),
                SEED_INSTANT
            )
        );
        InventoryItem eastItem = inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9244",
                "wh-east",
                new BigDecimal("5"),
                SEED_INSTANT
            )
        );
        InventoryItem westItem = inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9244",
                "wh-west",
                new BigDecimal("7"),
                SEED_INSTANT
            )
        );
        seedTransfer(mainItem, eastItem, "20", "17", "5", "8", "3", "Work order issue", "ops-a@arcanaerp.com", "WORK_ORDER", "WO-9244-1", "2027-06-07T01:00:00Z");
        seedTransfer(eastItem, westItem, "8", "6", "7", "9", "2", "Work order stage", "ops-b@arcanaerp.com", "WORK_ORDER", "WO-9244-1", "2027-06-07T02:00:00Z");
        seedTransfer(westItem, mainItem, "9", "5", "17", "21", "4", "Rebalance", "ops-c@arcanaerp.com", "REBALANCE", "REB-9244-1", "2027-06-14T03:00:00Z");

        mockMvc.perform(get("/api/inventory/{sku}/transfer-activity/daily-summary/by-reference", "arc-9244")
            .param("adjustedAtFrom", "2027-06-01T00:00:00Z")
            .param("adjustedAtTo", "2027-06-30T23:59:59Z")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.items[0].businessDate").value("2027-06-14"))
            .andExpect(jsonPath("$.items[0].referenceType").value("REBALANCE"))
            .andExpect(jsonPath("$.items[0].referenceId").value("REB-9244-1"))
            .andExpect(jsonPath("$.items[0].transferCount").value(1))
            .andExpect(jsonPath("$.items[0].totalQuantity").value(4))
            .andExpect(jsonPath("$.items[1].businessDate").value("2027-06-07"))
            .andExpect(jsonPath("$.items[1].referenceType").value("WORK_ORDER"))
            .andExpect(jsonPath("$.items[1].referenceId").value("WO-9244-1"))
            .andExpect(jsonPath("$.items[1].transferCount").value(2))
            .andExpect(jsonPath("$.items[1].totalQuantity").value(5));

        mockMvc.perform(get("/api/inventory/{sku}/transfer-activity/weekly-summary/by-reference", "arc-9244")
            .param("adjustedAtFrom", "2027-06-01T00:00:00Z")
            .param("adjustedAtTo", "2027-06-30T23:59:59Z")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.items[0].businessWeekStart").value("2027-06-14"))
            .andExpect(jsonPath("$.items[1].businessWeekStart").value("2027-06-07"))
            .andExpect(jsonPath("$.items[1].referenceId").value("WO-9244-1"))
            .andExpect(jsonPath("$.items[1].transferCount").value(2))
            .andExpect(jsonPath("$.items[1].totalQuantity").value(5));

        mockMvc.perform(get("/api/inventory/{sku}/transfer-activity/monthly-summary/by-reference", "arc-9244")
            .param("referenceType", "work_order")
            .param("referenceId", "WO-9244-1")
            .param("adjustedAtFrom", "2027-06-01T00:00:00Z")
            .param("adjustedAtTo", "2027-06-30T23:59:59Z")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].businessMonth").value("2027-06"))
            .andExpect(jsonPath("$.items[0].referenceType").value("WORK_ORDER"))
            .andExpect(jsonPath("$.items[0].referenceId").value("WO-9244-1"))
            .andExpect(jsonPath("$.items[0].transferCount").value(2))
            .andExpect(jsonPath("$.items[0].totalQuantity").value(5));

        mockMvc.perform(get("/api/inventory/{sku}/transfer-activity/daily-summary/by-reference", "arc-9244")
            .param("adjustedAtFrom", "2027-06-01T00:00:00Z")
            .param("adjustedAtTo", "2027-06-30T23:59:59Z")
            .param("page", "0")
            .param("size", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.items[0].businessDate").value("2027-06-14"))
            .andExpect(jsonPath("$.hasNext").value(true));
    }

    @Test
    void returnsTransferByTransferId() throws Exception {
        String payload = InventoryManagementWebTestSupport.transferPayload(
            "main",
            "wh-east",
            "4",
            "Fulfillment movement",
            DEFAULT_ACTOR,
            "fulfillment",
            "FUL-9214-1"
        );
        UUID transferId = createTransferScenarioTransferId(
            "arc-9214",
            TRANSFER_BY_ID_MAIN_ON_HAND,
            TRANSFER_BY_ID_EAST_ON_HAND,
            payload
        );

        mockMvc.perform(InventoryManagementWebTestSupport.transferByIdRequest(transferId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.transferId").value(transferId.toString()))
            .andExpect(jsonPath("$.sku").value("ARC-9214"))
            .andExpect(jsonPath("$.sourceLocationCode").value("MAIN"))
            .andExpect(jsonPath("$.destinationLocationCode").value("WH-EAST"))
            .andExpect(jsonPath("$.quantity").value(4))
            .andExpect(jsonPath("$.sourceOnHandQuantity").value(7))
            .andExpect(jsonPath("$.destinationOnHandQuantity").value(6))
            .andExpect(jsonPath("$.reason").value("Fulfillment movement"))
            .andExpect(jsonPath("$.adjustedBy").value(DEFAULT_ACTOR))
            .andExpect(jsonPath("$.referenceType").value("FULFILLMENT"))
            .andExpect(jsonPath("$.referenceId").value("FUL-9214-1"))
            .andExpect(jsonPath("$.transferredAt").isNotEmpty());
    }

    @Test
    void returnsNotFoundForUnknownTransferId() throws Exception {
        UUID unknownTransferId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        expectTransferNotFound(
            mockMvc.perform(InventoryManagementWebTestSupport.transferByIdRequest(unknownTransferId)),
            unknownTransferId,
            "/api/inventory/transfers/" + unknownTransferId
        );
    }

    @Test
    void reversesTransferByTransferIdWithCompensatingMovements() throws Exception {
        UUID originalTransferId = createTransferScenarioTransferId(
            "arc-9216",
            DEFAULT_MAIN_ON_HAND,
            DEFAULT_EAST_ON_HAND,
            defaultTransferPayload()
        );

        String reversalPayload = reversalPayload(DEFAULT_REVERSAL_REASON);

        InventoryManagementWebTestSupport.reverseTransfer(mockMvc, originalTransferId, reversalPayload)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.sku").value("ARC-9216"))
            .andExpect(jsonPath("$.sourceLocationCode").value("WH-EAST"))
            .andExpect(jsonPath("$.destinationLocationCode").value("MAIN"))
            .andExpect(jsonPath("$.quantity").value(3))
            .andExpect(jsonPath("$.sourceOnHandQuantity").value(4))
            .andExpect(jsonPath("$.destinationOnHandQuantity").value(10))
            .andExpect(jsonPath("$.reason").value(DEFAULT_REVERSAL_REASON))
            .andExpect(jsonPath("$.adjustedBy").value(DEFAULT_ACTOR))
            .andExpect(jsonPath("$.referenceType").value("TRANSFER_REVERSAL"))
            .andExpect(jsonPath("$.referenceId").value(originalTransferId.toString()));

        mockMvc.perform(get("/api/inventory/{sku}", "arc-9216"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.locationCode").value("MAIN"))
            .andExpect(jsonPath("$.onHandQuantity").value(10));

        mockMvc.perform(get("/api/inventory/{sku}", "arc-9216")
            .param("locationCode", "wh-east"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.locationCode").value("WH-EAST"))
            .andExpect(jsonPath("$.onHandQuantity").value(4));

        InventoryItem eastItem = inventoryItemRepository.findBySkuAndLocationCode("ARC-9216", "WH-EAST").orElseThrow();
        InventoryAdjustment reversalSource = inventoryAdjustmentRepository
            .findByInventoryItemIdOrderByAdjustedAtDesc(eastItem.getId())
            .getFirst();
        assertThat(reversalSource.getTransferId()).isNotEqualTo(originalTransferId);
        assertThat(reversalSource.getQuantityDelta()).isEqualByComparingTo("-3");
        assertThat(reversalSource.getReferenceType()).isEqualTo("TRANSFER_REVERSAL");
        assertThat(reversalSource.getReferenceId()).isEqualTo(originalTransferId.toString());

        List<InventoryAdjustment> reversalPair = inventoryAdjustmentRepository
            .findByTransferIdOrderByAdjustedAtAsc(reversalSource.getTransferId());
        assertThat(reversalPair).hasSize(2);
    }

    @Test
    void returnsNotFoundForUnknownTransferReversalRequest() throws Exception {
        UUID unknownTransferId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        String reversalPayload = reversalPayload(DEFAULT_REVERSAL_REASON);

        expectReversalTransferNotFound(
            InventoryManagementWebTestSupport.reverseTransfer(mockMvc, unknownTransferId, reversalPayload),
            unknownTransferId
        );
    }

    @Test
    void rejectsDuplicateReversalForSameTransferId() throws Exception {
        UUID originalTransferId = createLegacyTransferScenarioTransferId("arc-9219");
        String reversalPayload = reversalPayload(DEFAULT_REVERSAL_REASON);

        InventoryManagementWebTestSupport.reverseTransfer(mockMvc, originalTransferId, reversalPayload)
            .andExpect(status().isCreated());

        expectDuplicateReversalConflict(
            InventoryManagementWebTestSupport.reverseTransfer(mockMvc, originalTransferId, reversalPayload),
            originalTransferId
        );
    }

    @Test
    void retriesReversalWithIdempotencyKeyReturnsOriginalResponse() throws Exception {
        String reversalPayload = reversalPayload(DEFAULT_REVERSAL_REASON);
        ReversalScenario reversalScenario = scenarioWithReversal("arc-9220", "reverse-9220-a", reversalPayload);
        UUID originalTransferId = reversalScenario.originalTransferId();
        UUID reversalTransferId = reversalScenario.reversalTransferId();

        expectIdempotentReplay(
            originalTransferId,
            "reverse-9220-a",
            reversalPayload,
            reversalTransferId
        );
    }

    @Test
    void retriesReversalWithIdempotencyKeyWhenAdjustedByOnlyDiffersByCaseReturnsOriginalResponse() throws Exception {
        String firstReversalPayload = reversalPayload(DEFAULT_REVERSAL_REASON, "OPS@ARCANAERP.COM");
        String replayReversalPayload = reversalPayload(DEFAULT_REVERSAL_REASON);
        ReversalScenario reversalScenario = scenarioWithReversal(
            "arc-9220b",
            "reverse-9220b-a",
            firstReversalPayload,
            result -> result
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.adjustedBy").value(DEFAULT_ACTOR))
        );
        UUID originalTransferId = reversalScenario.originalTransferId();
        UUID reversalTransferId = reversalScenario.reversalTransferId();

        expectIdempotentReplay(
            originalTransferId,
            "reverse-9220b-a",
            replayReversalPayload,
            reversalTransferId,
            result -> result.andExpect(jsonPath("$.adjustedBy").value(DEFAULT_ACTOR)),
            result -> result
                .andExpect(jsonPath("$.items[0].transferId").value(reversalTransferId.toString()))
                .andExpect(jsonPath("$.items[0].adjustedBy").value(DEFAULT_ACTOR))
        );
    }

    @Test
    void rejectsBlankIdempotencyKeyHeaderOnReversalRequest() throws Exception {
        IdempotencyScenario scenario = createIdempotencyScenario("arc-9221");
        UUID originalTransferId = scenario.originalTransferId();
        String reversalPayload = reversalPayload(DEFAULT_REVERSAL_REASON);

        expectBadRequest(
            InventoryManagementWebTestSupport.reverseTransfer(
                mockMvc,
                originalTransferId,
                "   ",
                reversalPayload
            ),
            "Idempotency-Key header must not be blank",
            "/api/inventory/transfers/" + originalTransferId + "/reversals"
        );
    }

    @Test
    void retriesReversalWhenIdempotencyKeyHeaderOnlyDiffersBySurroundingWhitespace() throws Exception {
        String reversalPayload = reversalPayload(DEFAULT_REVERSAL_REASON);
        ReversalScenario reversalScenario = scenarioWithReversal("arc-9221b", " reverse-9221b-a ", reversalPayload);
        UUID originalTransferId = reversalScenario.originalTransferId();
        UUID reversalTransferId = reversalScenario.reversalTransferId();

        expectIdempotentReplay(
            originalTransferId,
            "reverse-9221b-a",
            reversalPayload,
            reversalTransferId,
            result -> result.andExpect(jsonPath("$.items[0].transferId").value(reversalTransferId.toString()))
        );
    }

    @Test
    void rejectsIdempotencyKeyReuseWithDifferentPayloadWhenHeaderOnlyDiffersBySurroundingWhitespace() throws Exception {
        ReversalScenario reversalScenario = scenarioWithReversal(
            "arc-9221c",
            " reverse-9221c-a ",
            reversalPayload(DEFAULT_REVERSAL_REASON)
        );
        UUID originalTransferId = reversalScenario.originalTransferId();
        String secondReversalPayload = reversalPayloadWithDifferentReason();

        expectIdempotencyPayloadConflict(
            InventoryManagementWebTestSupport.reverseTransfer(
                mockMvc,
                originalTransferId,
                "reverse-9221c-a",
                secondReversalPayload
            ),
            originalTransferId
        );

        expectSingleReversalHistory(originalTransferId);
    }

    @Test
    void rejectsIdempotencyKeyReuseWithDifferentReversalPayload() throws Exception {
        Arc9222Scenario scenario = arc9222Scenario("");
        ReversalScenario reversalScenario = arc9222ScenarioWithReversal(scenario, reversalPayload(DEFAULT_REVERSAL_REASON));
        UUID originalTransferId = reversalScenario.originalTransferId();
        String secondReversalPayload = reversalPayloadWithDifferentReason();

        expectIdempotencyPayloadConflictForScenario(scenario, originalTransferId, secondReversalPayload);

        expectSingleReversalHistory(originalTransferId);
    }

    @Test
    void rejectsIdempotencyKeyReuseWhenReasonOnlyDiffersByCase() throws Exception {
        Arc9222Scenario scenario = arc9222Scenario("c");
        ReversalScenario reversalScenario = arc9222ScenarioWithReversal(scenario, reversalPayload(DEFAULT_REVERSAL_REASON));
        UUID originalTransferId = reversalScenario.originalTransferId();
        String secondReversalPayload = reversalPayloadLowercaseReason();

        expectIdempotencyPayloadConflictForScenario(scenario, originalTransferId, secondReversalPayload);

        expectSingleReversalHistory(originalTransferId);
    }

    @Test
    void retriesReversalWithIdempotencyKeyWhenReasonOnlyDiffersByTrailingWhitespaceReturnsOriginalResponse() throws Exception {
        Arc9222Scenario scenario = arc9222Scenario("d");
        String firstReversalPayload = reversalPayload(DEFAULT_REVERSAL_REASON);
        String secondReversalPayload = reversalPayloadWithTrailingWhitespaceReason();
        ReversalScenario reversalScenario = arc9222ScenarioWithReversal(
            scenario,
            firstReversalPayload,
            result -> result
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reason").value(DEFAULT_REVERSAL_REASON))
        );
        UUID originalTransferId = reversalScenario.originalTransferId();
        UUID reversalTransferId = reversalScenario.reversalTransferId();

        expectIdempotentReplay(
            originalTransferId,
            scenario.key(),
            secondReversalPayload,
            reversalTransferId,
            result -> result.andExpect(jsonPath("$.reason").value(DEFAULT_REVERSAL_REASON)),
            result -> result
                .andExpect(jsonPath("$.items[0].transferId").value(reversalTransferId.toString()))
                .andExpect(jsonPath("$.items[0].reason").value(DEFAULT_REVERSAL_REASON))
        );
    }

    @Test
    void rejectsIdempotencyKeyReuseWhenAdjustedByValueDiffers() throws Exception {
        Arc9222Scenario scenario = arc9222Scenario("b");
        ReversalScenario reversalScenario = arc9222ScenarioWithReversal(scenario, reversalPayload(DEFAULT_REVERSAL_REASON));
        UUID originalTransferId = reversalScenario.originalTransferId();
        String secondReversalPayload = reversalPayloadWithWarehouseActor();

        expectIdempotencyPayloadConflictForScenario(scenario, originalTransferId, secondReversalPayload);

        expectSingleReversalHistory(originalTransferId);
    }

    @Test
    void reasonCaseOnlyChangesConflictWhileAdjustedByCaseOnlyChangesReplay() throws Exception {
        Arc9222Scenario reasonCaseScenarioDef = arc9222Scenario("e");
        ReversalScenario reasonCaseScenario = arc9222ScenarioWithReversal(
            reasonCaseScenarioDef,
            reversalPayload(DEFAULT_REVERSAL_REASON)
        );
        UUID reasonCaseTransferId = reasonCaseScenario.originalTransferId();

        expectIdempotencyPayloadConflictForScenario(
            reasonCaseScenarioDef,
            reasonCaseTransferId,
            reversalPayloadWithTitleCaseReason()
        );
        expectSingleReversalHistory(reasonCaseTransferId);

        Arc9222Scenario adjustedByCaseScenarioDef = arc9222Scenario("f");
        ReversalScenario adjustedByCaseScenario = arc9222ScenarioWithReversal(
            adjustedByCaseScenarioDef,
            reversalPayloadWithMixedCaseActor()
        );
        UUID adjustedByCaseTransferId = adjustedByCaseScenario.originalTransferId();
        UUID adjustedByCaseReversalId = adjustedByCaseScenario.reversalTransferId();

        expectIdempotentReplay(
            adjustedByCaseTransferId,
            adjustedByCaseScenarioDef.key(),
            reversalPayloadWithUppercaseActor(),
            adjustedByCaseReversalId,
            result -> result.andExpect(jsonPath("$.adjustedBy").value(DEFAULT_ACTOR)),
            result -> result
                .andExpect(jsonPath("$.items[0].transferId").value(adjustedByCaseReversalId.toString()))
                .andExpect(jsonPath("$.items[0].adjustedBy").value(DEFAULT_ACTOR))
        );
    }

    @Test
    void concurrentFirstWriteWithSameIdempotencyKeyReturnsConflictForOneRequest() throws Exception {
        IdempotencyScenario scenario = createIdempotencyScenario("arc-9223");
        UUID originalTransferId = scenario.originalTransferId();
        String reversalPayload = reversalPayload(DEFAULT_REVERSAL_REASON);

        CountDownLatch firstClaimBlocked = new CountDownLatch(1);
        CountDownLatch releaseFirstClaim = new CountDownLatch(1);
        AtomicBoolean firstInvocation = new AtomicBoolean(true);
        doAnswer(invocation -> {
            if (firstInvocation.compareAndSet(true, false)) {
                firstClaimBlocked.countDown();
                if (!releaseFirstClaim.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Timed out waiting to release first idempotency claim");
                }
            }
            InventoryTransferReversalIdempotency entity = invocation.getArgument(0);
            try {
                entityManager.persist(entity);
                entityManager.flush();
                return entity;
            } catch (PersistenceException exception) {
                throw new DataIntegrityViolationException("idempotency key claim conflict", exception);
            }
        }).when(reversalIdempotencyRepository).saveAndFlush(any(InventoryTransferReversalIdempotency.class));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        Callable<Integer> reverseCall = () -> InventoryManagementWebTestSupport.reverseTransfer(
                mockMvc,
                originalTransferId,
                "reverse-9223-race",
                reversalPayload
            )
            .andReturn()
            .getResponse()
            .getStatus();

        try {
            Future<Integer> first = executor.submit(reverseCall);
            assertThat(firstClaimBlocked.await(10, TimeUnit.SECONDS)).isTrue();

            Future<Integer> second = executor.submit(reverseCall);
            int secondStatus = second.get(15, TimeUnit.SECONDS);

            releaseFirstClaim.countDown();
            int firstStatus = first.get(15, TimeUnit.SECONDS);

            assertThat(secondStatus).isEqualTo(HttpStatus.CREATED.value());
            assertThat(firstStatus).isEqualTo(HttpStatus.CONFLICT.value());

            expectSingleReversalHistory(originalTransferId);
        } finally {
            releaseFirstClaim.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void retriesAfterStalePendingClaimRecoversAndReturnsOriginalReversal() throws Exception {
        IdempotencyScenario scenario = createIdempotencyScenario("arc-9224");
        UUID originalTransferId = scenario.originalTransferId();
        String reversalPayload = reversalPayload(DEFAULT_REVERSAL_REASON);

        InventoryManagementWebTestSupport.reverseTransfer(mockMvc, originalTransferId, reversalPayload)
            .andExpect(status().isCreated());

        InventoryItem mainItem = inventoryItemRepository.findBySkuAndLocationCode(scenario.sku().toUpperCase(), "MAIN").orElseThrow();
        UUID existingReversalTransferId = inventoryAdjustmentRepository
            .findByInventoryItemIdOrderByAdjustedAtDesc(mainItem.getId())
            .stream()
            .filter(adjustment ->
                "TRANSFER_REVERSAL".equals(adjustment.getReferenceType()) &&
                originalTransferId.toString().equals(adjustment.getReferenceId())
            )
            .findFirst()
            .orElseThrow()
            .getTransferId();

        seedStalePendingClaim(originalTransferId, "reverse-9224-stale");

        InventoryManagementWebTestSupport.reverseTransfer(
            mockMvc,
            originalTransferId,
            "reverse-9224-stale",
            reversalPayload
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.transferId").value(existingReversalTransferId.toString()))
            .andExpect(jsonPath("$.referenceType").value("TRANSFER_REVERSAL"))
            .andExpect(jsonPath("$.referenceId").value(originalTransferId.toString()));

        InventoryTransferReversalIdempotency idempotency = reversalIdempotencyRepository
            .findByTransferIdAndIdempotencyKey(originalTransferId, "reverse-9224-stale")
            .orElseThrow();
        assertThat(idempotency.getReversalTransferId()).isEqualTo(existingReversalTransferId);
        assertThat(idempotency.getReversalTransferId()).isNotEqualTo(PENDING_REVERSAL_TRANSFER_ID);

        expectSingleReversalHistory(originalTransferId);
    }

    @Test
    void retriesAfterStalePendingClaimRelinksExistingReversalWhenIdempotencyKeyOnlyDiffersBySurroundingWhitespace()
        throws Exception {
        IdempotencyScenario scenario = createIdempotencyScenario("arc-9224c");
        UUID originalTransferId = scenario.originalTransferId();
        String reversalPayload = reversalPayload(DEFAULT_REVERSAL_REASON);
        String seededStaleIdempotencyKey = " reverse-9224c-stale ";
        String replayIdempotencyKey = "reverse-9224c-stale";

        InventoryManagementWebTestSupport.reverseTransfer(mockMvc, originalTransferId, reversalPayload)
            .andExpect(status().isCreated());

        InventoryItem mainItem = inventoryItemRepository.findBySkuAndLocationCode(scenario.sku().toUpperCase(), "MAIN").orElseThrow();
        UUID existingReversalTransferId = inventoryAdjustmentRepository
            .findByInventoryItemIdOrderByAdjustedAtDesc(mainItem.getId())
            .stream()
            .filter(adjustment ->
                "TRANSFER_REVERSAL".equals(adjustment.getReferenceType()) &&
                originalTransferId.toString().equals(adjustment.getReferenceId())
            )
            .findFirst()
            .orElseThrow()
            .getTransferId();

        seedStalePendingClaim(originalTransferId, seededStaleIdempotencyKey);

        InventoryManagementWebTestSupport.reverseTransfer(
            mockMvc,
            originalTransferId,
            replayIdempotencyKey,
            reversalPayload
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.transferId").value(existingReversalTransferId.toString()))
            .andExpect(jsonPath("$.referenceType").value("TRANSFER_REVERSAL"))
            .andExpect(jsonPath("$.referenceId").value(originalTransferId.toString()));

        InventoryTransferReversalIdempotency idempotency = reversalIdempotencyRepository
            .findByTransferIdAndIdempotencyKey(originalTransferId, replayIdempotencyKey)
            .orElseThrow();
        assertThat(idempotency.getReversalTransferId()).isEqualTo(existingReversalTransferId);
        assertThat(idempotency.getReversalTransferId()).isNotEqualTo(PENDING_REVERSAL_TRANSFER_ID);

        expectSingleReversalHistory(originalTransferId);
    }

    @Test
    void reclaimsStalePendingClaimAndCreatesReversalWhenMissing() throws Exception {
        IdempotencyScenario scenario = createIdempotencyScenario("arc-9224a");
        UUID originalTransferId = scenario.originalTransferId();
        String reversalPayload = reversalPayload(DEFAULT_REVERSAL_REASON);
        String staleIdempotencyKey = "reverse-9224a-stale";

        expectStaleReplayCreatesReversal(
            scenario,
            staleIdempotencyKey,
            staleIdempotencyKey,
            reversalPayload,
            result -> {},
            (result, createdReversalTransferId) -> {}
        );
    }

    @Test
    void reclaimsStalePendingClaimWithTrimEquivalentIdempotencyKeyAndCreatesReversalWhenMissing() throws Exception {
        IdempotencyScenario scenario = createIdempotencyScenario("arc-9224b");
        UUID originalTransferId = scenario.originalTransferId();
        String reversalPayload = reversalPayload(DEFAULT_REVERSAL_REASON);
        String seededStaleIdempotencyKey = " reverse-9224b-stale ";
        String replayIdempotencyKey = "reverse-9224b-stale";

        expectTrimEquivalentStaleReplayCreatesReversal(
            scenario,
            seededStaleIdempotencyKey,
            replayIdempotencyKey,
            reversalPayload,
            result -> {},
            (result, createdReversalTransferId) -> {}
        );
    }

    @Test
    void rejectsStalePendingClaimReplayWhenPayloadFingerprintDiffersAndKeepsPendingClaimUnchanged() throws Exception {
        IdempotencyScenario scenario = createIdempotencyScenario("arc-9224d");
        UUID originalTransferId = scenario.originalTransferId();
        String staleIdempotencyKey = "reverse-9224d-stale";
        String conflictingPayload = reversalPayloadWithDifferentReason();

        seedStalePendingClaim(originalTransferId, staleIdempotencyKey);
        expectStaleConflictWithoutReversalSideEffects(originalTransferId, staleIdempotencyKey, conflictingPayload);
    }

    @Test
    void rejectsTrimEquivalentStalePendingClaimReplayWhenPayloadFingerprintDiffersAndKeepsPendingClaimUnchanged()
        throws Exception {
        IdempotencyScenario scenario = createIdempotencyScenario("arc-9224e");
        UUID originalTransferId = scenario.originalTransferId();
        String seededStaleIdempotencyKey = " reverse-9224e-stale ";
        String replayIdempotencyKey = "reverse-9224e-stale";
        String conflictingPayload = reversalPayloadWithDifferentReason();

        expectTrimEquivalentStaleConflictWithoutReversalSideEffects(
            originalTransferId,
            seededStaleIdempotencyKey,
            replayIdempotencyKey,
            conflictingPayload
        );
    }

    @Test
    void reclaimsStalePendingClaimWhenAdjustedByOnlyDiffersByCaseAndReplaysIdempotently() throws Exception {
        IdempotencyScenario scenario = createIdempotencyScenario("arc-9224f");
        UUID originalTransferId = scenario.originalTransferId();
        String staleIdempotencyKey = "reverse-9224f-stale";
        String replayPayload = reversalPayloadWithUppercaseActor();

        expectStaleReplayCreatesReversal(
            scenario,
            staleIdempotencyKey,
            staleIdempotencyKey,
            replayPayload,
            result -> result.andExpect(jsonPath("$.adjustedBy").value(DEFAULT_ACTOR)),
            (result, createdReversalTransferId) -> result.andExpect(jsonPath("$.items[0].adjustedBy").value(DEFAULT_ACTOR))
        );
    }

    @Test
    void reclaimsStalePendingClaimWhenReasonOnlyDiffersByTrailingWhitespaceAndReplaysIdempotently() throws Exception {
        IdempotencyScenario scenario = createIdempotencyScenario("arc-9224g");
        UUID originalTransferId = scenario.originalTransferId();
        String staleIdempotencyKey = "reverse-9224g-stale";
        String replayPayload = reversalPayloadWithTrailingWhitespaceReason();

        expectStaleReplayCreatesReversal(
            scenario,
            staleIdempotencyKey,
            staleIdempotencyKey,
            replayPayload,
            result -> result.andExpect(jsonPath("$.reason").value(DEFAULT_REVERSAL_REASON)),
            (result, createdReversalTransferId) -> result.andExpect(jsonPath("$.items[0].reason").value(DEFAULT_REVERSAL_REASON))
        );
    }

    @Test
    void rejectsStalePendingClaimReplayWhenReasonOnlyDiffersByTitleCaseAndKeepsPendingClaimUnchanged() throws Exception {
        IdempotencyScenario scenario = createIdempotencyScenario("arc-9224h");
        UUID originalTransferId = scenario.originalTransferId();
        String staleIdempotencyKey = "reverse-9224h-stale";
        String conflictingPayload = reversalPayloadWithTitleCaseReason();

        seedStalePendingClaim(originalTransferId, staleIdempotencyKey);
        expectStaleConflictWithoutReversalSideEffects(originalTransferId, staleIdempotencyKey, conflictingPayload);
    }

    @Test
    void reclaimsTrimEquivalentStalePendingClaimWhenAdjustedByOnlyDiffersByCaseAndReplaysIdempotently() throws Exception {
        IdempotencyScenario scenario = createIdempotencyScenario("arc-9224i");
        UUID originalTransferId = scenario.originalTransferId();
        String seededStaleIdempotencyKey = " reverse-9224i-stale ";
        String replayIdempotencyKey = "reverse-9224i-stale";
        String replayPayload = reversalPayloadWithUppercaseActor();

        expectTrimEquivalentStaleReplayCreatesReversal(
            scenario,
            seededStaleIdempotencyKey,
            replayIdempotencyKey,
            replayPayload,
            result -> result.andExpect(jsonPath("$.adjustedBy").value(DEFAULT_ACTOR)),
            (result, createdReversalTransferId) -> result.andExpect(jsonPath("$.items[0].adjustedBy").value(DEFAULT_ACTOR))
        );
    }

    @Test
    void reclaimsTrimEquivalentStalePendingClaimWhenReasonOnlyDiffersByTrailingWhitespaceAndReplaysIdempotently()
        throws Exception {
        IdempotencyScenario scenario = createIdempotencyScenario("arc-9224j");
        UUID originalTransferId = scenario.originalTransferId();
        String seededStaleIdempotencyKey = " reverse-9224j-stale ";
        String replayIdempotencyKey = "reverse-9224j-stale";
        String replayPayload = reversalPayloadWithTrailingWhitespaceReason();

        expectTrimEquivalentStaleReplayCreatesReversal(
            scenario,
            seededStaleIdempotencyKey,
            replayIdempotencyKey,
            replayPayload,
            result -> result.andExpect(jsonPath("$.reason").value(DEFAULT_REVERSAL_REASON)),
            (result, createdReversalTransferId) -> result.andExpect(jsonPath("$.items[0].reason").value(DEFAULT_REVERSAL_REASON))
        );
    }

    @Test
    void rejectsTrimEquivalentStalePendingClaimReplayWhenReasonOnlyDiffersByTitleCaseAndKeepsPendingClaimUnchanged()
        throws Exception {
        IdempotencyScenario scenario = createIdempotencyScenario("arc-9224k");
        UUID originalTransferId = scenario.originalTransferId();
        String seededStaleIdempotencyKey = " reverse-9224k-stale ";
        String replayIdempotencyKey = "reverse-9224k-stale";
        String conflictingPayload = reversalPayloadWithTitleCaseReason();

        expectTrimEquivalentStaleConflictWithoutReversalSideEffects(
            originalTransferId,
            seededStaleIdempotencyKey,
            replayIdempotencyKey,
            conflictingPayload
        );
    }

    @Test
    void rejectsTrimEquivalentStalePendingClaimReplayWhenReasonDiffersAndKeepsPendingClaimUnchanged()
        throws Exception {
        IdempotencyScenario scenario = createIdempotencyScenario("arc-9224l");
        UUID originalTransferId = scenario.originalTransferId();
        String seededStaleIdempotencyKey = " reverse-9224l-stale ";
        String replayIdempotencyKey = "reverse-9224l-stale";
        String conflictingPayload = reversalPayloadWithDifferentReason();

        expectTrimEquivalentStaleConflictWithoutReversalSideEffects(
            originalTransferId,
            seededStaleIdempotencyKey,
            replayIdempotencyKey,
            conflictingPayload
        );
    }

    @Test
    void listsReversalHistoryForTransferId() throws Exception {
        UUID originalTransferId = createLegacyTransferScenarioTransferId("arc-9217");
        String reversalPayload = reversalPayload(DEFAULT_REVERSAL_REASON);

        InventoryManagementWebTestSupport.reverseTransfer(mockMvc, originalTransferId, reversalPayload)
            .andExpect(status().isCreated());

        expectSingleReversalHistory(
            originalTransferId,
            result -> result
                .andExpect(jsonPath("$.items[0].sku").value("ARC-9217"))
                .andExpect(jsonPath("$.items[0].sourceLocationCode").value("WH-EAST"))
                .andExpect(jsonPath("$.items[0].destinationLocationCode").value("MAIN"))
                .andExpect(jsonPath("$.items[0].quantity").value(3))
                .andExpect(jsonPath("$.items[0].reason").value(DEFAULT_REVERSAL_REASON))
                .andExpect(jsonPath("$.items[0].adjustedBy").value(DEFAULT_ACTOR))
                .andExpect(jsonPath("$.items[0].referenceType").value("TRANSFER_REVERSAL"))
                .andExpect(jsonPath("$.items[0].referenceId").value(originalTransferId.toString()))
        );
    }

    @Test
    void paginatesReversalHistoryAtPageBoundaries() throws Exception {
        IdempotencyScenario scenario = createIdempotencyScenario("arc-9217b");
        UUID originalTransferId = scenario.originalTransferId();
        String reversalPayload = reversalPayload(DEFAULT_REVERSAL_REASON);

        InventoryManagementWebTestSupport.reverseTransfer(mockMvc, originalTransferId, reversalPayload)
            .andExpect(status().isCreated());
        UUID reversalTransferId = latestReversalTransferId(scenario);

        mockMvc.perform(InventoryTransferReversalHistoryWebTestSupport.reversalsRequest(originalTransferId, 0, 1))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(1))
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.totalPages").value(1))
            .andExpect(jsonPath("$.hasNext").value(false))
            .andExpect(jsonPath("$.hasPrevious").value(false))
            .andExpect(jsonPath("$.items[0].transferId").value(reversalTransferId.toString()))
            .andExpect(jsonPath("$.items[0].referenceId").value(originalTransferId.toString()));

        mockMvc.perform(InventoryTransferReversalHistoryWebTestSupport.reversalsRequest(originalTransferId, 1, 1))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(1))
            .andExpect(jsonPath("$.size").value(1))
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.totalPages").value(1))
            .andExpect(jsonPath("$.hasNext").value(false))
            .andExpect(jsonPath("$.hasPrevious").value(true))
            .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void listsReversalHistoryWithControllerDefaultPaginationWhenParamsOmitted() throws Exception {
        IdempotencyScenario scenario = createIdempotencyScenario("arc-9217c");
        UUID originalTransferId = scenario.originalTransferId();
        String reversalPayload = reversalPayload(DEFAULT_REVERSAL_REASON);

        InventoryManagementWebTestSupport.reverseTransfer(mockMvc, originalTransferId, reversalPayload)
            .andExpect(status().isCreated());
        UUID reversalTransferId = latestReversalTransferId(scenario);

        mockMvc.perform(InventoryTransferReversalHistoryWebTestSupport.reversalsRequest(originalTransferId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.totalPages").value(1))
            .andExpect(jsonPath("$.hasNext").value(false))
            .andExpect(jsonPath("$.hasPrevious").value(false))
            .andExpect(jsonPath("$.items[0].transferId").value(reversalTransferId.toString()))
            .andExpect(jsonPath("$.items[0].referenceId").value(originalTransferId.toString()));
    }

    @Test
    void returnsEmptyReversalHistoryWhenNoReversalExists() throws Exception {
        UUID originalTransferId = createLegacyTransferScenarioTransferId("arc-9218");

        mockMvc.perform(InventoryTransferReversalHistoryWebTestSupport.reversalsRequestDefault(originalTransferId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(0))
            .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void returnsNotFoundForUnknownTransferReversalHistory() throws Exception {
        UUID unknownTransferId = UUID.fromString("33333333-3333-3333-3333-333333333333");

        expectReversalTransferNotFound(
            mockMvc.perform(InventoryTransferReversalHistoryWebTestSupport.reversalsRequestDefault(unknownTransferId)),
            unknownTransferId
        );
    }

    @Test
    void rejectsReversalHistoryWhenPageIsNegative() throws Exception {
        UUID transferId = UUID.fromString("44444444-4444-4444-4444-444444444444");

        expectBadRequest(
            mockMvc.perform(InventoryTransferReversalHistoryWebTestSupport.reversalsRequest(transferId, -1, 10)),
            "page must be greater than or equal to zero",
            "/api/inventory/transfers/" + transferId + "/reversals"
        );
    }

    @Test
    void rejectsReversalHistoryWhenSizeOutsideBounds() throws Exception {
        UUID transferId = UUID.fromString("55555555-5555-5555-5555-555555555555");

        expectBadRequest(
            mockMvc.perform(InventoryTransferReversalHistoryWebTestSupport.reversalsRequest(transferId, 0, 0)),
            "size must be between 1 and 100",
            "/api/inventory/transfers/" + transferId + "/reversals"
        );

        expectBadRequest(
            mockMvc.perform(InventoryTransferReversalHistoryWebTestSupport.reversalsRequest(transferId, 0, 101)),
            "size must be between 1 and 100",
            "/api/inventory/transfers/" + transferId + "/reversals"
        );
    }

    @Test
    void rejectsTransferWhenLocationsMatch() throws Exception {
        inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9209",
                "main",
                new BigDecimal("5"),
                SEED_INSTANT
            )
        );

        String payload = InventoryManagementWebTestSupport.transferPayload(
            "main",
            "main",
            "1",
            "Invalid transfer",
            DEFAULT_ACTOR
        );

        expectBadRequest(
            InventoryManagementWebTestSupport.transferInventory(mockMvc, "arc-9209", payload),
            "sourceLocationCode and destinationLocationCode must be different",
            "/api/inventory/arc-9209/transfers"
        );
    }

    @Test
    void rejectsTransferWithInsufficientSourceOnHand() throws Exception {
        inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9210",
                "main",
                new BigDecimal("2"),
                SEED_INSTANT
            )
        );

        String payload = InventoryManagementWebTestSupport.transferPayload(
            "main",
            "wh-west",
            "5",
            "Too large transfer",
            DEFAULT_ACTOR
        );

        expectBadRequest(
            InventoryManagementWebTestSupport.transferInventory(mockMvc, "arc-9210", payload),
            "onHandQuantity cannot become negative",
            "/api/inventory/arc-9210/transfers"
        );
    }

    @Test
    void returnsNotFoundWhenTransferringFromUnknownSourceLocation() throws Exception {
        String payload = InventoryManagementWebTestSupport.transferPayload(
            "wh-unknown",
            "main",
            "1",
            "Invalid transfer",
            DEFAULT_ACTOR
        );

        expectInventoryItemNotFound(
            InventoryManagementWebTestSupport.transferInventory(mockMvc, "arc-9211", payload),
            "arc-9211",
            "wh-unknown",
            "/api/inventory/arc-9211/transfers"
        );
    }

    @Test
    void rejectsTransferWhenQuantityIsZero() throws Exception {
        inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9212",
                "main",
                new BigDecimal("5"),
                SEED_INSTANT
            )
        );

        String payload = InventoryManagementWebTestSupport.transferPayload(
            "main",
            "wh-west",
            "0",
            "Invalid transfer",
            DEFAULT_ACTOR
        );

        expectBadRequest(
            InventoryManagementWebTestSupport.transferInventory(mockMvc, "arc-9212", payload),
            "quantity must be greater than zero",
            "/api/inventory/arc-9212/transfers"
        );
    }

    @Test
    void rejectsTransferWhenReferenceMetadataIsPartial() throws Exception {
        inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9213",
                "main",
                new BigDecimal("5"),
                SEED_INSTANT
            )
        );

        String payload = """
            {
              "sourceLocationCode": "main",
              "destinationLocationCode": "wh-west",
              "quantity": 1,
              "reason": "Invalid transfer",
              "adjustedBy": "%s",
              "referenceType": "FULFILLMENT"
            }
            """.formatted(DEFAULT_ACTOR);

        expectBadRequest(
            InventoryManagementWebTestSupport.transferInventory(mockMvc, "arc-9213", payload),
            "referenceType and referenceId must both be provided together",
            "/api/inventory/arc-9213/transfers"
        );
    }

    @Test
    void rejectsAdjustmentThatWouldMakeOnHandNegative() throws Exception {
        inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9205",
                "main",
                new BigDecimal("2"),
                SEED_INSTANT
            )
        );

        String payload = InventoryManagementWebTestSupport.adjustmentPayload(
            "-5",
            "Bad adjustment",
            DEFAULT_ACTOR
        );

        expectBadRequest(
            InventoryManagementWebTestSupport.adjustInventory(mockMvc, "arc-9205", payload),
            "onHandQuantity cannot become negative",
            "/api/inventory/arc-9205/adjustments"
        );
    }

    @Test
    void returnsNotFoundWhenAdjustingUnknownSkuAtLocation() throws Exception {
        String payload = InventoryManagementWebTestSupport.adjustmentPayload(
            "5",
            "Receiving posted",
            DEFAULT_ACTOR
        );

        expectInventoryItemNotFound(
            InventoryManagementWebTestSupport.adjustInventory(mockMvc, "arc-9206", "wh-west", payload),
            "arc-9206",
            "wh-west",
            "/api/inventory/arc-9206/adjustments"
        );
    }

    private IdempotencyScenario createIdempotencyScenario(String sku) throws Exception {
        UUID originalTransferId = createTransferScenarioTransferId(
            sku,
            DEFAULT_MAIN_ON_HAND,
            DEFAULT_EAST_ON_HAND,
            defaultTransferPayload()
        );
        return new IdempotencyScenario(sku, originalTransferId);
    }

    private UUID createTransferScenarioTransferId(
        String sku,
        BigDecimal mainOnHand,
        BigDecimal eastOnHand,
        String transferPayload
    ) throws Exception {
        InventoryIdempotencyTestFixture.seedTransferItems(
            inventoryItemRepository,
            sku,
            mainOnHand,
            eastOnHand,
            SEED_INSTANT
        );
        InventoryManagementWebTestSupport.transferInventory(mockMvc, sku, transferPayload)
            .andExpect(status().isCreated());
        return InventoryIdempotencyTestFixture.latestTransferIdFor(
            inventoryItemRepository,
            inventoryAdjustmentRepository,
            sku,
            "main"
        );
    }

    private UUID createLegacyTransferScenarioTransferId(String sku) throws Exception {
        return createIdempotencyScenario(sku).originalTransferId();
    }

    private static String defaultTransferPayload() {
        return InventoryManagementWebTestSupport.transferPayload(
            "main",
            "wh-east",
            "3",
            DEFAULT_TRANSFER_REASON,
            DEFAULT_ACTOR
        );
    }

    private UUID latestReversalTransferId(IdempotencyScenario scenario) {
        return InventoryIdempotencyTestFixture.latestTransferIdFor(
            inventoryItemRepository,
            inventoryAdjustmentRepository,
            scenario.sku(),
            "wh-east"
        );
    }

    private ReversalScenario scenarioWithReversal(
        String sku,
        String idempotencyKey,
        String reversalPayload
    ) throws Exception {
        return scenarioWithReversal(
            sku,
            idempotencyKey,
            reversalPayload,
            result -> result.andExpect(status().isCreated())
        );
    }

    private ReversalScenario arc9222ScenarioWithReversal(
        Arc9222Scenario scenario,
        String reversalPayload
    ) throws Exception {
        return scenarioWithReversal(scenario.sku(), scenario.key(), reversalPayload);
    }

    private ReversalScenario arc9222ScenarioWithReversal(
        Arc9222Scenario scenario,
        String reversalPayload,
        ReversalResponseExpectation expectation
    ) throws Exception {
        return scenarioWithReversal(scenario.sku(), scenario.key(), reversalPayload, expectation);
    }

    private void expectIdempotencyPayloadConflictForScenario(
        Arc9222Scenario scenario,
        UUID originalTransferId,
        String reversalPayload
    ) throws Exception {
        expectIdempotencyPayloadConflict(
            InventoryManagementWebTestSupport.reverseTransfer(
                mockMvc,
                originalTransferId,
                scenario.key(),
                reversalPayload
            ),
            originalTransferId
        );
    }

    private ReversalScenario scenarioWithReversal(
        String sku,
        String idempotencyKey,
        String reversalPayload,
        ReversalResponseExpectation expectation
    ) throws Exception {
        IdempotencyScenario scenario = createIdempotencyScenario(sku);
        var result = InventoryManagementWebTestSupport.reverseTransfer(
            mockMvc,
            scenario.originalTransferId(),
            idempotencyKey,
            reversalPayload
        );
        expectation.verify(result);
        return new ReversalScenario(scenario, latestReversalTransferId(scenario));
    }

    private void expectIdempotencyPayloadConflict(ResultActions result, UUID originalTransferId) throws Exception {
        expectConflict(
            result,
            "Idempotency-Key already used with different reversal payload for transferId: " + originalTransferId,
            "/api/inventory/transfers/" + originalTransferId + "/reversals"
        );
    }

    private void expectDuplicateReversalConflict(ResultActions result, UUID originalTransferId) throws Exception {
        expectConflict(result, "Inventory transfer already reversed: " + originalTransferId, "/api/inventory/transfers/" + originalTransferId + "/reversals");
    }

    private void expectConflict(ResultActions result, String message, String path) throws Exception {
        result
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.error").value("Conflict"))
            .andExpect(jsonPath("$.message").value(message))
            .andExpect(jsonPath("$.path").value(path));
    }

    private void expectReversalTransferNotFound(ResultActions result, UUID transferId) throws Exception {
        expectTransferNotFound(result, transferId, "/api/inventory/transfers/" + transferId + "/reversals");
    }

    private void expectTransferNotFound(ResultActions result, UUID transferId, String path) throws Exception {
        result
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value("Inventory transfer not found: " + transferId))
            .andExpect(jsonPath("$.path").value(path));
    }

    private void expectInventoryItemNotFound(
        ResultActions result,
        String sku,
        String locationCode,
        String path
    ) throws Exception {
        result
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(
                jsonPath("$.message")
                    .value(
                        "Inventory item not found for SKU: " + sku.toUpperCase() + " at location: " + locationCode.toUpperCase()
                    )
            )
            .andExpect(jsonPath("$.path").value(path));
    }

    private void expectInventoryLocationNotFound(ResultActions result, String code, String path) throws Exception {
        result
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value("Inventory location not found for code: " + code))
            .andExpect(jsonPath("$.path").value(path));
    }

    private void expectInventoryFacilityNotFound(ResultActions result, String code, String path) throws Exception {
        result
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value("Inventory facility not found for code: " + code))
            .andExpect(jsonPath("$.path").value(path));
    }

    private void expectInventoryFixedAssetNotFound(ResultActions result, String code, String path) throws Exception {
        result
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value("Inventory fixed asset not found for code: " + code))
            .andExpect(jsonPath("$.path").value(path));
    }

    private void expectInventoryFixedAssetTypeNotFound(ResultActions result, String code, String path) throws Exception {
        result
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value("Inventory fixed asset type not found for code: " + code))
            .andExpect(jsonPath("$.path").value(path));
    }

    private void expectBadRequest(ResultActions result, String message, String path) throws Exception {
        result
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value(message))
            .andExpect(jsonPath("$.path").value(path));
    }

    private void ensureUnitOfMeasurement(String code, String description) {
        try {
            unitOfMeasurementDirectory.registerUnitOfMeasurement(
                new RegisterUnitOfMeasurementCommand(code, description, "inventory", null)
            );
        } catch (ConflictException ignored) {
            // Shared Spring contexts can keep reference data from earlier tests.
        }
    }

    private void seedLocationType(String code, String description) {
        inventoryLocationTypeRepository.save(
            InventoryLocationType.create(code, description, SEED_INSTANT)
        );
    }

    private void seedFixedAssetType(String code, String description) {
        inventoryFixedAssetTypeRepository.save(
            InventoryFixedAssetType.create(code, description, SEED_INSTANT)
        );
    }

    private void seedInventoryParty(String code, String description) {
        inventoryPartyRepository.save(
            InventoryParty.create(code, description, SEED_INSTANT)
        );
    }

    private void seedInventoryPartyRoleType(String code, String description) {
        inventoryPartyRoleTypeRepository.save(
            InventoryPartyRoleType.create(code, description, SEED_INSTANT)
        );
    }

    private void seedInventoryEntryRelationshipReferenceData() {
        inventoryEntryRelationshipTypeRepository.save(
            InventoryEntryRelationshipType.create("component_of", "Component of", null, SEED_INSTANT)
        );
        seedInventoryEntryRoleType("component", "Component");
        seedInventoryEntryRoleType("assembly", "Assembly");
    }

    private void seedInventoryEntryRoleType(String code, String description) {
        inventoryEntryRoleTypeRepository.save(
            InventoryEntryRoleType.create(code, description, null, SEED_INSTANT)
        );
    }

    private String registerComponentRelationship(String toSku, String fromSku, String locationCode) throws Exception {
        seedInventoryEntryRelationshipReferenceData();
        inventoryItemRepository.save(InventoryItem.create(toSku, locationCode, new BigDecimal("3"), SEED_INSTANT));
        inventoryItemRepository.save(InventoryItem.create(fromSku, locationCode, new BigDecimal("12"), SEED_INSTANT));

        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
            "/api/inventory/entry-relationships"
        )
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {
                  "relationshipTypeCode": "component_of",
                  "fromSku": "%s",
                  "fromLocationCode": "%s",
                  "toSku": "%s",
                  "toLocationCode": "%s",
                  "fromRoleTypeCode": "component",
                  "toRoleTypeCode": "assembly",
                  "description": "Component participates in kit"
                }
                """.formatted(fromSku, locationCode, toSku, locationCode)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString()
            .replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");
    }

    private String registerProductInstanceAssignment(
        String sku,
        String locationCode,
        String productInstanceCode,
        String assignedBy
    ) throws Exception {
        inventoryItemRepository.save(InventoryItem.create(sku, locationCode, new BigDecimal("2"), SEED_INSTANT));

        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
            "/api/inventory/product-instance-assignments"
        )
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {
                  "sku": "%s",
                  "locationCode": "%s",
                  "productInstanceCode": "%s",
                  "assignedBy": "%s"
                }
                """.formatted(sku, locationCode, productInstanceCode, assignedBy)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString()
            .replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");
    }

    private String registerItemLocationAssignment(
        String sku,
        String itemLocationCode,
        String assignedLocationCode,
        String validFrom
    ) throws Exception {
        inventoryItemRepository.save(InventoryItem.create(sku, itemLocationCode, new BigDecimal("2"), SEED_INSTANT));
        inventoryLocationRepository.save(InventoryLocation.create(assignedLocationCode, assignedLocationCode, SEED_INSTANT));

        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
            "/api/inventory/item-location-assignments"
        )
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("""
                {
                  "sku": "%s",
                  "itemLocationCode": "%s",
                  "assignedLocationCode": "%s",
                  "validFrom": "%s",
                  "assignedBy": "inventory.manager"
                }
                """.formatted(sku, itemLocationCode, assignedLocationCode, validFrom)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString()
            .replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");
    }

    private void expectSingleReversalHistory(UUID originalTransferId) throws Exception {
        expectSingleReversalHistory(originalTransferId, result -> {});
    }

    private void expectIdempotentReplay(
        UUID originalTransferId,
        String idempotencyKey,
        String replayPayload,
        UUID reversalTransferId
    ) throws Exception {
        expectIdempotentReplay(originalTransferId, idempotencyKey, replayPayload, reversalTransferId, result -> {}, result -> {});
    }

    private void expectIdempotentReplay(
        UUID originalTransferId,
        String idempotencyKey,
        String replayPayload,
        UUID reversalTransferId,
        ReversalHistoryExpectation historyExpectation
    ) throws Exception {
        expectIdempotentReplay(
            originalTransferId,
            idempotencyKey,
            replayPayload,
            reversalTransferId,
            result -> {},
            historyExpectation
        );
    }

    private void expectIdempotentReplay(
        UUID originalTransferId,
        String idempotencyKey,
        String replayPayload,
        UUID reversalTransferId,
        ReversalResponseExpectation responseExpectation,
        ReversalHistoryExpectation historyExpectation
    ) throws Exception {
        ResultActions replayResult = InventoryManagementWebTestSupport.reverseTransfer(
            mockMvc,
            originalTransferId,
            idempotencyKey,
            replayPayload
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.transferId").value(reversalTransferId.toString()))
            .andExpect(jsonPath("$.referenceType").value("TRANSFER_REVERSAL"))
            .andExpect(jsonPath("$.referenceId").value(originalTransferId.toString()));
        responseExpectation.verify(replayResult);

        expectSingleReversalHistory(originalTransferId, historyExpectation);
    }

    private void expectSingleReversalHistory(
        UUID originalTransferId,
        ReversalHistoryExpectation expectation
    ) throws Exception {
        ResultActions result = mockMvc.perform(InventoryTransferReversalHistoryWebTestSupport.reversalsRequestDefault(originalTransferId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1));
        expectation.verify(result);
    }

    private void expectStaleConflictWithoutReversalSideEffects(
        UUID originalTransferId,
        String replayIdempotencyKey,
        String replayPayload
    ) throws Exception {
        expectIdempotencyPayloadConflict(
            InventoryManagementWebTestSupport.reverseTransfer(
                mockMvc,
                originalTransferId,
                replayIdempotencyKey,
                replayPayload
            ),
            originalTransferId
        );

        InventoryTransferReversalIdempotency idempotency = reversalIdempotencyRepository
            .findByTransferIdAndIdempotencyKey(originalTransferId, replayIdempotencyKey)
            .orElseThrow();
        assertThat(idempotency.getReversalTransferId()).isEqualTo(PENDING_REVERSAL_TRANSFER_ID);

        mockMvc.perform(InventoryTransferReversalHistoryWebTestSupport.reversalsRequestDefault(originalTransferId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(0));
    }

    private void expectTrimEquivalentStaleConflictWithoutReversalSideEffects(
        UUID originalTransferId,
        String seededStaleIdempotencyKey,
        String replayIdempotencyKey,
        String replayPayload
    ) throws Exception {
        seedStalePendingClaim(originalTransferId, seededStaleIdempotencyKey);
        expectStaleConflictWithoutReversalSideEffects(originalTransferId, replayIdempotencyKey, replayPayload);
    }

    private void expectTrimEquivalentStaleReplayCreatesReversal(
        IdempotencyScenario scenario,
        String seededStaleIdempotencyKey,
        String replayIdempotencyKey,
        String replayPayload,
        ReversalResponseExpectation responseExpectation,
        StaleReplayHistoryExpectation historyExpectation
    ) throws Exception {
        expectStaleReplayCreatesReversal(
            scenario,
            seededStaleIdempotencyKey,
            replayIdempotencyKey,
            replayPayload,
            responseExpectation,
            historyExpectation
        );
    }

    private void expectStaleReplayCreatesReversal(
        IdempotencyScenario scenario,
        String seededStaleIdempotencyKey,
        String replayIdempotencyKey,
        String replayPayload,
        ReversalResponseExpectation responseExpectation,
        StaleReplayHistoryExpectation historyExpectation
    ) throws Exception {
        UUID originalTransferId = scenario.originalTransferId();
        seedStalePendingClaim(originalTransferId, seededStaleIdempotencyKey);

        ResultActions replayResult = InventoryManagementWebTestSupport.reverseTransfer(
            mockMvc,
            originalTransferId,
            replayIdempotencyKey,
            replayPayload
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.referenceType").value("TRANSFER_REVERSAL"))
            .andExpect(jsonPath("$.referenceId").value(originalTransferId.toString()));
        responseExpectation.verify(replayResult);

        UUID createdReversalTransferId = latestReversalTransferId(scenario);
        InventoryTransferReversalIdempotency idempotency = reversalIdempotencyRepository
            .findByTransferIdAndIdempotencyKey(originalTransferId, replayIdempotencyKey)
            .orElseThrow();
        assertThat(idempotency.getReversalTransferId()).isEqualTo(createdReversalTransferId);
        assertThat(idempotency.getReversalTransferId()).isNotEqualTo(PENDING_REVERSAL_TRANSFER_ID);

        expectSingleReversalHistory(
            originalTransferId,
            result -> {
                result.andExpect(jsonPath("$.items[0].transferId").value(createdReversalTransferId.toString()));
                historyExpectation.verify(result, createdReversalTransferId);
            }
        );
    }

    private void seedStalePendingClaim(UUID originalTransferId, String idempotencyKey) {
        reversalIdempotencyRepository.saveAndFlush(
            InventoryTransferReversalIdempotency.create(
                originalTransferId,
                idempotencyKey,
                InventoryReversalFingerprintTestSupport.fingerprintForReversalRequest(
                    DEFAULT_REVERSAL_REASON,
                    DEFAULT_ACTOR
                ),
                PENDING_REVERSAL_TRANSFER_ID,
                STALE_PENDING_CLAIM_AT
            )
        );
    }

    private void seedAdjustment(
        InventoryItem item,
        String previousOnHandQuantity,
        String quantityDelta,
        String currentOnHandQuantity,
        String reason,
        String adjustedBy,
        String adjustedAt
    ) {
        inventoryAdjustmentRepository.save(
            InventoryAdjustment.create(
                item.getId(),
                item.getSku(),
                item.getLocationCode(),
                null,
                new BigDecimal(previousOnHandQuantity),
                new BigDecimal(quantityDelta),
                new BigDecimal(currentOnHandQuantity),
                reason,
                adjustedBy,
                null,
                null,
                Instant.parse(adjustedAt)
            )
        );
    }

    private void seedTransfer(
        InventoryItem sourceItem,
        InventoryItem destinationItem,
        String sourcePreviousOnHandQuantity,
        String sourceCurrentOnHandQuantity,
        String destinationPreviousOnHandQuantity,
        String destinationCurrentOnHandQuantity,
        String quantity,
        String reason,
        String adjustedBy,
        String referenceType,
        String referenceId,
        String adjustedAt
    ) {
        UUID transferId = UUID.randomUUID();
        Instant transferredAt = Instant.parse(adjustedAt);
        inventoryAdjustmentRepository.save(
            InventoryAdjustment.create(
                sourceItem.getId(),
                sourceItem.getSku(),
                sourceItem.getLocationCode(),
                transferId,
                new BigDecimal(sourcePreviousOnHandQuantity),
                new BigDecimal(quantity).negate(),
                new BigDecimal(sourceCurrentOnHandQuantity),
                reason,
                adjustedBy,
                referenceType,
                referenceId,
                transferredAt
            )
        );
        inventoryAdjustmentRepository.save(
            InventoryAdjustment.create(
                destinationItem.getId(),
                destinationItem.getSku(),
                destinationItem.getLocationCode(),
                transferId,
                new BigDecimal(destinationPreviousOnHandQuantity),
                new BigDecimal(quantity),
                new BigDecimal(destinationCurrentOnHandQuantity),
                reason,
                adjustedBy,
                referenceType,
                referenceId,
                transferredAt
            )
        );
    }

    private record IdempotencyScenario(String sku, UUID originalTransferId) {}
    private record ReversalScenario(IdempotencyScenario scenario, UUID reversalTransferId) {
        private UUID originalTransferId() {
            return scenario.originalTransferId();
        }
    }
    @FunctionalInterface
    private interface ReversalResponseExpectation {
        void verify(ResultActions result) throws Exception;
    }

    @FunctionalInterface
    private interface ReversalHistoryExpectation {
        void verify(ResultActions result) throws Exception;
    }

    @FunctionalInterface
    private interface StaleReplayHistoryExpectation {
        void verify(ResultActions result, UUID createdReversalTransferId) throws Exception;
    }

    private static String reversalPayload(String reason) {
        return reversalPayload(reason, DEFAULT_ACTOR);
    }

    private static String reversalPayload(String reason, String adjustedBy) {
        return InventoryManagementWebTestSupport.reversalPayload(reason, adjustedBy);
    }

    private static String reversalPayloadWithDifferentReason() {
        return reversalPayload(DEFAULT_REVERSAL_REASON + " with different reason");
    }

    private static String reversalPayloadLowercaseReason() {
        return reversalPayload("reversal posted");
    }

    private static String reversalPayloadWithTrailingWhitespaceReason() {
        return reversalPayload(DEFAULT_REVERSAL_REASON + " ");
    }

    private static String reversalPayloadWithTitleCaseReason() {
        return reversalPayload("Reversal Posted");
    }

    private static String reversalPayloadWithWarehouseActor() {
        return reversalPayload(DEFAULT_REVERSAL_REASON, "warehouse@arcanaerp.com");
    }

    private static String reversalPayloadWithUppercaseActor() {
        return reversalPayload(DEFAULT_REVERSAL_REASON, "OPS@ARCANAERP.COM");
    }

    private static String reversalPayloadWithMixedCaseActor() {
        return reversalPayload(DEFAULT_REVERSAL_REASON, "Ops@ArcanaERP.com");
    }

    private static Arc9222Scenario arc9222Scenario(String suffix) {
        return new Arc9222Scenario(suffix);
    }

    private record Arc9222Scenario(String suffix) {
        private String sku() {
            return "arc-9222" + suffix;
        }

        private String key() {
            return "reverse-9222" + (suffix.isEmpty() ? "-a" : suffix + "-a");
        }
    }

}
