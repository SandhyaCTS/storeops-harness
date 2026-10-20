package com.storeops;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
                .andExpect(jsonPath("$.status").value("TODO"));
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

    @Test
    void patchUpdatesFieldsAndFilterFindsActivity() throws Exception {
        String id = createActivity("store-1");
        mvc.perform(patch("/api/activities/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"BLOCKED\",\"priority\":\"CRITICAL\",\"assigneeId\":\"s2\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"))
                .andExpect(jsonPath("$.priority").value("CRITICAL"))
                .andExpect(jsonPath("$.assigneeId").value("s2"))
                .andExpect(jsonPath("$.title").value("Restock aisle 3"));
        mvc.perform(get("/api/activities").param("status", "BLOCKED"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[?(@.id=='" + id + "')]", hasSize(1)));
        mvc.perform(get("/api/activities").param("programmeId", "no-such-programme"))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(patch("/api/activities/nope").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteAllowedForOwnerOrStoreManagerOnly() throws Exception {
        String body = mvc.perform(post("/api/activities").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"storeId\":\"store-1\",\"title\":\"Audit\",\"ownerId\":\"s2\"}"))
                .andReturn().getResponse().getContentAsString();
        String id = mapper.readTree(body).get("id").asText();
        mvc.perform(delete("/api/activities/" + id).header("X-Staff-Id", "someone-else"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mvc.perform(delete("/api/activities/" + id)).andExpect(status().isForbidden());
        mvc.perform(delete("/api/activities/" + id).header("X-Staff-Id", "s2")).andExpect(status().isNoContent());
        mvc.perform(get("/api/activities/" + id)).andExpect(status().isNotFound());

        String id2 = createActivity("store-1");
        mvc.perform(delete("/api/activities/" + id2).header("X-Staff-Id", "s1")).andExpect(status().isNoContent());
    }

    @Test
    void addsMemberToProgrammeAndRejectsDuplicatesAndUnknownStaff() throws Exception {
        String body = mvc.perform(post("/api/programmes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Refit\",\"storeId\":\"store-1\"}"))
                .andReturn().getResponse().getContentAsString();
        String id = mapper.readTree(body).get("id").asText();
        String member = "{\"staffId\":\"s2\",\"role\":\"ASSOCIATE\"}";
        mvc.perform(post("/api/programmes/" + id + "/members").contentType(MediaType.APPLICATION_JSON)
                        .content(member))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.members", hasSize(1)));
        mvc.perform(post("/api/programmes/" + id + "/members").contentType(MediaType.APPLICATION_JSON)
                        .content(member))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/programmes/" + id + "/members").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"staffId\":\"ghost\",\"role\":\"ASSOCIATE\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/programmes/missing/members").contentType(MediaType.APPLICATION_JSON)
                        .content(member))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/programmes").param("storeId", "store-1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[?(@.id=='" + id + "')]", hasSize(1)));
    }
}
