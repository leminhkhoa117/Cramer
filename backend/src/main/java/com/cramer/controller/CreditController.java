package com.cramer.controller;

import com.cramer.dto.CreditHistoryDTO;
import com.cramer.dto.CreditTransactionDTO;
import com.cramer.dto.LuaPurchaseDTO;
import com.cramer.dto.LuaPurchaseResponseDTO;
import com.cramer.dto.PageDTO;
import com.cramer.dto.UserCreditDTO;
import com.cramer.dto.UserFullStatsDTO;
import com.cramer.service.CreditService;
import com.cramer.service.LuaCreditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Credit (Lúa) management.
 * Provides endpoints for credit balance, transactions, packages, and purchases.
 */
@RestController
@RequestMapping("/api/credits")
@Tag(name = "Credit (Lúa) Management", description = "APIs for managing user Lúa credits and transactions")
public class CreditController extends BaseController {

    private static final Logger logger = LoggerFactory.getLogger(CreditController.class);

    private final CreditService creditService;
    private final LuaCreditService luaCreditService;

    @Autowired
    public CreditController(CreditService creditService, LuaCreditService luaCreditService) {
        this.creditService = creditService;
        this.luaCreditService = luaCreditService;
    }


    @Operation(
        summary = "Get user's credit balance",
        description = "Retrieve the authenticated user's Lúa balance and lifetime statistics"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Balance retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping
    public ResponseEntity<UserCreditDTO> getBalance(Authentication authentication) {
        UUID userId = getCurrentUserId(authentication);
        logger.info("💰 GET /api/credits - User: {}", userId);
        
        UserCreditDTO credits = creditService.getBalance(userId);
        return ResponseEntity.ok(credits);
    }

    @Operation(
        summary = "Check if user has enough credits",
        description = "Check if the user has sufficient Lúa balance for a specific amount"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Check completed"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/check/{amount}")
    public ResponseEntity<Boolean> hasEnoughCredits(
            @Parameter(description = "Amount to check") @PathVariable @jakarta.validation.constraints.Min(1) int amount,
            Authentication authentication) {
        UUID userId = getCurrentUserId(authentication);
        logger.info("🔍 GET /api/credits/check/{} - User: {}", amount, userId);
        
        boolean hasEnough = creditService.hasEnoughCredits(userId, amount);
        return ResponseEntity.ok(hasEnough);
    }

    @Operation(
        summary = "Get transaction history",
        description = "Retrieve the user's credit transaction history with pagination"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Transactions retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/transactions")
    public ResponseEntity<PageDTO<CreditTransactionDTO>> getTransactionHistory(
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") @jakarta.validation.constraints.Min(0) int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(100) int size,
            Authentication authentication) {
        UUID userId = getCurrentUserId(authentication);
        logger.info("📜 GET /api/credits/transactions - User: {}, page: {}, size: {}", userId, page, size);
        
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<CreditTransactionDTO> transactions = creditService.getTransactionHistory(userId, pageable);
        
        PageDTO<CreditTransactionDTO> pageDTO = new PageDTO<>(
                transactions.getContent(),
                transactions.getNumber(),
                transactions.getSize(),
                transactions.getTotalElements(),
                transactions.getTotalPages()
        );
        
        return ResponseEntity.ok(pageDTO);
    }

    @Operation(
        summary = "Get aggregated user statistics",
        description = "Retrieve comprehensive user statistics including subscription, credits, streaks, and achievements"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Stats retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/stats")
    public ResponseEntity<UserFullStatsDTO> getUserStats(Authentication authentication) {
        UUID userId = getCurrentUserId(authentication);
        logger.info("📊 GET /api/credits/stats - User: {}", userId);
        
        UserFullStatsDTO stats = creditService.getUserStats(userId);
        return ResponseEntity.ok(stats);
    }

    // ========================================
    // LÚA PACKAGE ENDPOINTS
    // ========================================

    @Operation(
        summary = "Get available Lúa packages",
        description = "Retrieve list of Lúa packages available for purchase with pricing and bonus info"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Packages retrieved successfully")
    })
    @GetMapping("/packages")
    public ResponseEntity<List<LuaCreditService.LuaPackage>> getAvailablePackages() {
        logger.info("📦 GET /api/credits/packages");
        List<LuaCreditService.LuaPackage> packages = luaCreditService.getAvailablePackages();
        return ResponseEntity.ok(packages);
    }

    @Operation(
        summary = "Initiate Lúa package purchase",
        description = "Create a PayOS payment link for purchasing a Lúa package"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Payment link created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid package code"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/purchase")
    public ResponseEntity<LuaPurchaseResponseDTO> purchasePackage(
            @Valid @RequestBody LuaPurchaseDTO request,
            Authentication authentication) {
        UUID userId = getCurrentUserId(authentication);
        logger.info("💰 POST /api/credits/purchase - User: {}, Package: {}", userId, request.getPackageCode());
        
        LuaPurchaseResponseDTO response = luaCreditService.initiatePurchase(userId, request.getPackageCode());
        
        if (response.getSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @Operation(
        summary = "Get transaction history with filter",
        description = "Retrieve credit transaction history with optional type filter (all/earn/spend)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "History retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/history")
    public ResponseEntity<PageDTO<CreditHistoryDTO>> getHistory(
            @Parameter(description = "Filter type: all, earn, or spend")
            @RequestParam(defaultValue = "all") @jakarta.validation.constraints.Pattern(regexp = "all|earn|spend") String type,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") @jakarta.validation.constraints.Min(0) int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(100) int size,
            Authentication authentication) {
        UUID userId = getCurrentUserId(authentication);
        logger.info("📜 GET /api/credits/history - User: {}, Type: {}, Page: {}", userId, type, page);
        
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<CreditHistoryDTO> history = luaCreditService.getTransactionHistory(userId, type, pageable);
        
        PageDTO<CreditHistoryDTO> pageDTO = new PageDTO<>(
                history.getContent(),
                history.getNumber(),
                history.getSize(),
                history.getTotalElements(),
                history.getTotalPages()
        );
        
        return ResponseEntity.ok(pageDTO);
    }
}

