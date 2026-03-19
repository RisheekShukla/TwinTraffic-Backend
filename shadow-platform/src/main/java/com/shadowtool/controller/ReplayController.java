package com.shadowtool.controller;

import com.shadowtool.dto.ProxyResponseDTO;
import com.shadowtool.service.ReplayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/replay")
@RequiredArgsConstructor
@Tag(name = "Replay", description = "Replay stored requests through the proxy")
public class ReplayController {

    private final ReplayService replayService;

    @PostMapping("/{id}")
    @Operation(
        summary = "Replay a stored request",
        description = "Re-sends the original request to v1 and v2, creates a new comparison record"
    )
    public ResponseEntity<ProxyResponseDTO> replay(@PathVariable UUID id) {
        ProxyResponseDTO response = replayService.replay(id);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }
}
