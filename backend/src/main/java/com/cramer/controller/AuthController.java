package com.cramer.controller;

import com.cramer.dto.CheckEmailRequest;
import com.cramer.service.SupabaseAdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final SupabaseAdminService supabaseAdminService;

    public AuthController(SupabaseAdminService supabaseAdminService) {
        this.supabaseAdminService = supabaseAdminService;
    }

    @PostMapping("/check-email")
    public ResponseEntity<Map<String, Boolean>> checkEmail(@RequestBody CheckEmailRequest request) {
        if (request == null || request.getEmail() == null || request.getEmail().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        boolean exists = supabaseAdminService.checkEmailExists(request.getEmail());
        return ResponseEntity.ok(Map.of("exists", exists));
    }
}
