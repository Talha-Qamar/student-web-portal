package com.studentportal.controller;

import com.studentportal.dto.TranscriptResponse;
import com.studentportal.service.TranscriptService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transcript")
public class TranscriptController {

    private final TranscriptService transcriptService;

    public TranscriptController(TranscriptService transcriptService) {
        this.transcriptService = transcriptService;
    }

    @GetMapping("/{studentId}")
    public ResponseEntity<TranscriptResponse> getTranscript(@PathVariable("studentId") Long studentId) {
        return ResponseEntity.ok(transcriptService.getTranscript(studentId));
    }
}
