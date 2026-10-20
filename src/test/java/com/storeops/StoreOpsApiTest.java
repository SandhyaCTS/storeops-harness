package com.storeops;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class StoreOpsApiTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    private String createActivity(String storeId) throws Exception {
        String body = mvc.perform(post("/api/activities").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"storeId\":\"" + storeId + "\",\"title\":\"Restock aisle 3\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = mapper.readTree(body);
        return node.get("id").asText();
    }

    @Test
    void createsAndFetchesActivity() throws Exception {
        String id = createActivity("store-1");
        mvc.perform(get("/api/activities/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"));
        mvc.perform(get("/api/activities")).andExpect(status().isOk());
    }

    @Test
    void unknownActivityReturnsTypedError() throws Exception {
        mvc.perform(get("/api/activities/nope")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.statusCode").value(404));
    }

    @Test
    void invalidActivityReturnsValidationError() throws Exception {
        mvc.perform(post("/api/activities").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void createsAndListsProgrammes() throws Exception {
        mvc.perform(post("/api/programmes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Summer Sale\"}"))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/programmes")).andExpect(status().isOk());
    }

    @Test
    void listsStaff() throws Exception {
        mvc.perform(get("/api/staff")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    void activityCreationRaisesAlertViaEventBusAndCanBeAcknowledged() throws Exception {
        createActivity("store-9");
        String body = mvc.perform(get("/api/alerts")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String alertId = mapper.readTree(body).get(0).get("id").asText();
        mvc.perform(post("/api/alerts/" + alertId + "/acknowledge")).andExpect(status().isOk())
                .andExpect(jsonPath("$.acknowledged").value(true));
        mvc.perform(post("/api/alerts/missing/acknowledge")).andExpect(status().isNotFound());
    }

    @Test
    void storeReportAggregatesReadOnlyData() throws Exception {
        mvc.perform(get("/api/reports/stores/store-1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.staffCount").value(2));
    }
}
