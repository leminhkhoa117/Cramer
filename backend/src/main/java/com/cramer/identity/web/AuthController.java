package com.cramer.identity.web;

import com.cramer.identity.service.EmailLookupService;
import com.cramer.identity.web.dto.CheckEmailRequest;
import com.cramer.identity.web.dto.CheckEmailResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public auth helper endpoints (SPEC-10 §2). Authentication itself is performed by Supabase on
 * the client; this only answers "does an account already exist for this email".
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final EmailLookupService emailLookup;

    public AuthController(EmailLookupService emailLookup) {
        this.emailLookup = emailLookup;
    }

    @PostMapping("/check-email")
    public CheckEmailResponse checkEmail(@Valid @RequestBody CheckEmailRequest request) {
        return new CheckEmailResponse(emailLookup.emailExists(request.email()));
    }
}
