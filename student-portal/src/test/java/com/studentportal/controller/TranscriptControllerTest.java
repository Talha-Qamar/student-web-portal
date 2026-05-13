package com.studentportal.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studentportal.dto.TranscriptResponse;
import com.studentportal.service.TranscriptService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TranscriptControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TranscriptController(new StubTranscriptService())).build();

    @Test
    void getTranscriptDelegatesToService() throws Exception {
        mockMvc.perform(get("/api/transcript/1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentName").value("Ali Khan"))
                .andExpect(jsonPath("$.overallGpa").value(3.5));
    }

    private static class StubTranscriptService extends TranscriptService {

        private StubTranscriptService() {
            super(null, null, null);
        }

        @Override
        public TranscriptResponse getTranscript(Long studentId) {
            return new TranscriptResponse("Ali Khan", "CS", 2022, 3.5, 90, java.util.List.of(), java.util.List.of());
        }
    }
}