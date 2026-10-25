package com.storeops.activities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.storeops.common.event.ActivityUpdatedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Bulk status endpoint: rejections have no side effects, success updates all and publishes events. */
@SpringBootTest
@AutoConfigureMockMvc
@RecordApplicationEvents
class BulkStatusTest {

    private static final String URL = "/api/activities/bulk-status";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private ApplicationEvents events;

    private String create(String title) throws Exception {
        String body = mvc.perform(post("/api/activities").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"storeId\":\"store-1\",\"title\":\"" + title
                                + "\",\"priority\":\"HIGH\",\"category\":\"AUDIT\","
                                + "\"assigneeId\":\"s2\",\"ownerId\":\"s2\"}"))
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(body).get("id").asText();
    }

    private ResultActions bulk(String staffId, String json) throws Exception {
        var req = post(URL).contentType(MediaType.APPLICATION_JSON).content(json);
        if (staffId != null) {
            req = req.header("X-Staff-Id", staffId);
        }
        return mvc.perform(req);
    }

    private JsonNode fetch(String id) throws Exception {
        return mapper.readTree(mvc.perform(get("/api/activities/" + id)).andReturn().getResponse()
                .getContentAsString());
    }

    private void assertTodoAndNoEvents(String... ids) throws Exception {
        for (String id : ids) {
            assertThat(fetch(id).get("status").asText()).isEqualTo("TODO");
        }
        assertThat(events.stream(ActivityUpdatedEvent.class).count()).isZero();
    }

    private static String body(String ids, String status) {
        return "{\"activityIds\":" + ids + (status == null ? "" : ",\"status\":" + status) + "}";
    }

    @Test
    void managerUpdatesAllInOrderAndPublishesOneEventEach() throws Exception {
        String a1 = create("A1");
        String a2 = create("A2");
        JsonNode before1 = fetch(a1);
        JsonNode before2 = fetch(a2);
        bulk("s1", body("[\"" + a2 + "\",\"" + a1 + "\"]", "\"DONE\""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(a2))
                .andExpect(jsonPath("$[0].status").value("DONE"))
                .andExpect(jsonPath("$[1].id").value(a1))
                .andExpect(jsonPath("$[1].status").value("DONE"));
        for (String id : new String[] {a1, a2}) {
            JsonNode after = fetch(id);
            JsonNode before = id.equals(a1) ? before1 : before2;
            assertThat(after.get("status").asText()).isEqualTo("DONE");
            for (String field : new String[] {"priority", "category", "assigneeId", "ownerId", "title"}) {
                assertThat(after.get(field)).isEqualTo(before.get(field));
            }
        }
        assertThat(events.stream(ActivityUpdatedEvent.class).toList()).containsExactly(
                new ActivityUpdatedEvent(a2, "store-1", "TODO", "DONE"),
                new ActivityUpdatedEvent(a1, "store-1", "TODO", "DONE"));
    }

    @Test
    void anyStatusToAnyOtherSucceeds() throws Exception {
        String a1 = create("A1");
        bulk("s1", body("[\"" + a1 + "\"]", "\"DONE\"")).andExpect(status().isOk());
        bulk("s1", body("[\"" + a1 + "\"]", "\"TODO\"")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("TODO"));
        assertThat(fetch(a1).get("status").asText()).isEqualTo("TODO");
        assertThat(events.stream(ActivityUpdatedEvent.class).toList()).containsExactly(
                new ActivityUpdatedEvent(a1, "store-1", "TODO", "DONE"),
                new ActivityUpdatedEvent(a1, "store-1", "DONE", "TODO"));
    }

    @Test
    void nonManagerMissingHeaderAndUnknownStaffAreForbidden() throws Exception {
        String a1 = create("A1");
        String json = body("[\"" + a1 + "\"]", "\"DONE\"");
        bulk("s2", json).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
        bulk(null, json).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
        bulk("nobody", json).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
        assertTodoAndNoEvents(a1);
    }

    @Test
    void permissionIsCheckedBeforeIds() throws Exception {
        bulk("s2", body("[\"missing\"]", "\"DONE\"")).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        assertThat(events.stream(ActivityUpdatedEvent.class).count()).isZero();
    }

    @Test
    void unknownIdIsNotFoundNamingFirstUnknownAndChangesNothing() throws Exception {
        String a1 = create("A1");
        String a2 = create("A2");
        bulk("s1", body("[\"" + a1 + "\",\"missing\",\"" + a2 + "\"]", "\"DONE\""))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("missing")));
        bulk("s1", body("[\"" + a1 + "\",\"gone-1\",\"gone-2\"]", "\"DONE\""))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("gone-1")))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("gone-2"))));
        assertTodoAndNoEvents(a1, a2);
    }

    @Test
    void invalidBodiesAreValidationErrorsAndChangeNothing() throws Exception {
        String a1 = create("A1");
        bulk("s1", body("[]", "\"DONE\"")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        bulk("s1", "{\"status\":\"DONE\"}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        bulk("s1", "{\"activityIds\":null,\"status\":\"DONE\"}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        bulk("s1", body("[\"" + a1 + "\"]", null)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        bulk("s1", body("[\"" + a1 + "\"]", "null")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        assertTodoAndNoEvents(a1);
    }
}
