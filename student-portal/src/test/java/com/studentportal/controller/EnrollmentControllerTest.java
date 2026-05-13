package com.studentportal.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studentportal.dto.DropCourseRequest;
import com.studentportal.dto.RegisterCourseRequest;
import com.studentportal.model.Enrollment;
import com.studentportal.service.EnrollmentService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EnrollmentControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new EnrollmentController(new StubEnrollmentService())).build();

    @Test
    void listRegisterAndDropDelegateToService() throws Exception {
        mockMvc.perform(get("/api/enrollments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").exists());

        mockMvc.perform(post("/api/enrollments/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerBody())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/enrollments/drop")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dropBody())))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    private java.util.Map<String, Object> registerBody() {
        return java.util.Map.of("studentId", 1, "courseId", 10);
    }

    private java.util.Map<String, Object> dropBody() {
        return java.util.Map.of("studentId", 1, "courseId", 10);
    }

    private static class StubEnrollmentService extends EnrollmentService {

        private StubEnrollmentService() {
            super(null, null, null, null);
        }

        @Override
        public List<Enrollment> getEnrollmentsForStudent(Long studentId) {
            return List.of(new Enrollment());
        }

        @Override
        public Enrollment registerCourse(RegisterCourseRequest request) {
            return new Enrollment();
        }

        @Override
        public void dropCourse(DropCourseRequest request) {
        }
    }
}