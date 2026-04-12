package com.arcanaerp.platform.core.uom.web;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class UnitsOfMeasurementControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsAndListsUnitsOfMeasurement() throws Exception {
        CoreWebIntegrationTestSupport.createUnitOfMeasurement(
            mockMvc,
            "wkg",
            "Web Kilogram",
            "web_weight",
            "Base mass unit"
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.code").value("WKG"))
            .andExpect(jsonPath("$.description").value("Web Kilogram"))
            .andExpect(jsonPath("$.domain").value("WEB_WEIGHT"));

        CoreWebIntegrationTestSupport.createUnitOfMeasurement(
            mockMvc,
            "wkm",
            "Web Kilometer",
            "web_length",
            "Base length unit"
        )
            .andExpect(status().isCreated());

        mockMvc.perform(
            CoreWebIntegrationTestSupport.listUnitsOfMeasurementRequest(
                0,
                10,
                "queryFilter",
                "Web Ki",
                "domain",
                "web_length"
            )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(10))
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[?(@.code=='WKM')].description", hasItem("Web Kilometer")));
    }

    @Test
    void usesDefaultPaginationWhenPageAndSizeOmitted() throws Exception {
        CoreWebIntegrationTestSupport.createUnitOfMeasurement(
            mockMvc,
            "wlb",
            "Pound",
            "weight",
            null
        )
            .andExpect(status().isCreated());

        mockMvc.perform(CoreWebIntegrationTestSupport.listUnitsOfMeasurementRequest())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.totalItems", greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.items[?(@.code=='WLB')].description", hasItem("Pound")));
    }

    @Test
    void rejectsDuplicateCode() throws Exception {
        CoreWebIntegrationTestSupport.createUnitOfMeasurement(
            mockMvc,
            "wm",
            "Meter",
            "length",
            null
        )
            .andExpect(status().isCreated());

        CoreWebIntegrationTestSupport.createUnitOfMeasurement(
            mockMvc,
            "WM",
            "Meter Copy",
            "length",
            null
        )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.error").value("Conflict"))
            .andExpect(jsonPath("$.message").value("Unit of measurement code already exists: WM"))
            .andExpect(jsonPath("$.path").value("/api/core/units-of-measurement"));
    }

    @Test
    void rejectsInvalidFiltersAndPagination() throws Exception {
        mockMvc.perform(CoreWebIntegrationTestSupport.listUnitsOfMeasurementRequest(0, 10, "queryFilter", "   "))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("queryFilter must not be blank"));

        mockMvc.perform(CoreWebIntegrationTestSupport.listUnitsOfMeasurementRequest(0, 10, "domain", "   "))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("domain must not be blank"));

        mockMvc.perform(CoreWebIntegrationTestSupport.listUnitsOfMeasurementRequest(-1, 10))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("page must be greater than or equal to zero"));
    }
}
