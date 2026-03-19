package com.shadowtool.controller;

import com.shadowtool.dto.ProxyRequestDTO;
import com.shadowtool.dto.ProxyResponseDTO;
import com.shadowtool.service.ProxyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/proxy")
@RequiredArgsConstructor
@Tag(name = "Proxy", description = "Shadow traffic proxy gateway")
public class ProxyController {

    private final ProxyService proxyService;

    @PostMapping
    @Operation(
        summary = "Proxy a request",
        description = "Forwards request to v1 (primary) and asynchronously mirrors it to v2 (shadow). " +
                      "Returns v1 response immediately."
    )
    public ResponseEntity<ProxyResponseDTO> proxy(@Valid @RequestBody ProxyRequestDTO request) {
        ProxyResponseDTO response = proxyService.proxy(request);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }
}
