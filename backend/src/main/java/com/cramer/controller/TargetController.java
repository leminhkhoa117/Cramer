package com.cramer.controller;

import com.cramer.dto.TargetDTO;
import com.cramer.service.TargetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/targets")
@RequiredArgsConstructor
public class TargetController {

    private final TargetService targetService;

    @GetMapping("/me")
    public ResponseEntity<TargetDTO> getMyTarget(@AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        return targetService.getTargetByUserId(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/me")
    public ResponseEntity<TargetDTO> createOrUpdateMyTarget(@AuthenticationPrincipal UserDetails userDetails, @RequestBody TargetDTO targetDTO) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        TargetDTO savedTarget = targetService.createOrUpdateTarget(targetDTO, userId);
        return ResponseEntity.ok(savedTarget);
    }
}
