package com.storeops;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storeops.common.event.ActivityDeletedEvent;
import com.storeops.common.event.ActivityUpdatedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;

/** Every activity state change must reach the event bus. */
@SpringBootTest
@AutoConfigureMockMvc
@RecordApplicationEvents
class ActivityEventsTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private ApplicationEvents events;

    private String createActivity() throws Exception {
        String body = mvc.perform(post("/api/activities").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"storeId\":\"store-1\",\"title\":\"Audit\",\"ownerId\":\"s2\"}"))
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(body).get("id").asText();
    }

    @Test
    void patchPublishesUpdatedEventWithStatusTransition() throws Exception {
        String id = createActivity();
        mvc.perform(patch("/api/activities/" + id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"DONE\"}")).andExpect(status().isOk());
        assertThat(events.stream(ActivityUpdatedEvent.class).toList())
                .containsExactly(new ActivityUpdatedEvent(id, "store-1", "TODO", "DONE"));
    }

    @Test
    void deletePublishesDeletedEventOnlyWhenAllowed() throws Exception {
        String id = createActivity();
        mvc.perform(delete("/api/activities/" + id).header("X-Staff-Id", "intruder"))
                .andExpect(status().isForbidden());
        assertThat(events.stream(ActivityDeletedEvent.class).count()).isZero();
        mvc.perform(delete("/api/activities/" + id).header("X-Staff-Id", "s2")).andExpect(status().isNoContent());
        assertThat(events.stream(ActivityDeletedEvent.class).toList())
                .containsExactly(new ActivityDeletedEvent(id, "store-1"));
    }

    @Test
    void failedPatchPublishesNothing() throws Exception {
        mvc.perform(patch("/api/activities/nope").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
        assertThat(events.stream(ActivityUpdatedEvent.class).count()).isZero();
    }
}
