package com.cramer.service.implement;

import com.cramer.dto.CreditHistoryDTO;
import com.cramer.dto.LuaPackDTO;
import com.cramer.dto.LuaPurchaseResponseDTO;
import com.cramer.dto.PaymentResponseDTO;
import com.cramer.entity.CreditTransaction;
import com.cramer.entity.LuaPack;
import com.cramer.repository.CreditTransactionRepository;
import com.cramer.repository.LuaPackRepository;
import com.cramer.service.LuaCreditService;
import com.cramer.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of LuaCreditService.
 * Manages Lúa package definitions and purchase flow.
 * 
 * Now reads packages from the database (lua_packs table) instead of hardcoded values.
 */
@Service
public class LuaCreditServiceImpl implements LuaCreditService {

    private static final Logger logger = LoggerFactory.getLogger(LuaCreditServiceImpl.class);

    private final LuaPackRepository luaPackRepository;
    private final PaymentService paymentService;
    private final CreditTransactionRepository transactionRepository;

    @Autowired
    public LuaCreditServiceImpl(LuaPackRepository luaPackRepository,
                                 PaymentService paymentService,
                                 CreditTransactionRepository transactionRepository) {
        this.luaPackRepository = luaPackRepository;
        this.paymentService = paymentService;
        this.transactionRepository = transactionRepository;
    }

    @Override
    public List<LuaPackage> getAvailablePackages() {
        // Read from database and convert to LuaPackage record for backward compatibility
        return luaPackRepository.findAllActive().stream()
                .map(this::entityToRecord)
                .collect(Collectors.toList());
    }
    
    /**
     * Get all available Lúa packs as DTOs (new API).
     */
    public List<LuaPackDTO> getAvailablePacks() {
        return luaPackRepository.findAllActive().stream()
                .map(LuaPackDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public LuaPackage getPackageByCode(String packageCode) {
        return luaPackRepository.findByCodeAndIsActiveTrue(packageCode)
                .map(this::entityToRecord)
                .orElse(null);
    }
    
    /**
     * Get a pack by code as DTO (new API).
     */
    public LuaPackDTO getPackByCode(String packCode) {
        return luaPackRepository.findByCodeAndIsActiveTrue(packCode)
                .map(LuaPackDTO::fromEntity)
                .orElse(null);
    }
    
    /**
     * Convert LuaPack entity to legacy LuaPackage record.
     */
    private LuaPackage entityToRecord(LuaPack entity) {
        return new LuaPackage(
                entity.getCode(),
                entity.getNameVi(),
                entity.getLuaAmount(),
                entity.getPriceVnd(),
                entity.getDiscountPercent()
        );
    }

    @Override
    public LuaPurchaseResponseDTO initiatePurchase(UUID userId, String packageCode) {
        logger.info("🌙 Initiating Lúa purchase - User: {}, Package: {}", userId, packageCode);

        // Get pack from database
        LuaPack pack = luaPackRepository.findByCodeAndIsActiveTrue(packageCode).orElse(null);
        if (pack == null) {
            logger.warn("❌ Invalid package code: {}", packageCode);
            return LuaPurchaseResponseDTO.builder()
                    .success(false)
                    .message("Gói Lúa không hợp lệ: " + packageCode)
                    .build();
        }

        try {
            // Delegate to existing PaymentService
            PaymentResponseDTO paymentResponse = paymentService.createLuaPackPayment(
                    userId,
                    pack.getTotalLua(),  // Total lua including bonus
                    pack.getPriceVnd()
            );

            logger.info("✅ Payment link created - Order: {}, URL: {}",
                    paymentResponse.getOrderCode(),
                    paymentResponse.getCheckoutUrl() != null ? "generated" : "null");

            return LuaPurchaseResponseDTO.builder()
                    .success(true)
                    .packageCode(pack.getCode())
                    .packageName(pack.getNameVi())
                    .luaAmount(pack.getLuaAmount())
                    .bonusAmount(pack.getBonusLua())
                    .totalLua(pack.getTotalLua())
                    .priceVnd(pack.getPriceVnd())
                    .bonusPercent(pack.getDiscountPercent())
                    .checkoutUrl(paymentResponse.getCheckoutUrl())
                    .orderCode(paymentResponse.getOrderCode())
                    .message("Đang chuyển hướng đến trang thanh toán...")
                    .build();

        } catch (Exception e) {
            logger.error("❌ Failed to create payment link for user {} package {}: {}",
                    userId, packageCode, e.getMessage());
            return LuaPurchaseResponseDTO.builder()
                    .success(false)
                    .packageCode(pack.getCode())
                    .message("Không thể tạo link thanh toán: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public void completePurchase(Long orderCode, int luaAmount) {
        // NOTE: This is handled by the existing PaymentServiceImpl webhook handler
        // which calls CreditService.earnCredits() with category PURCHASE.
        // This method is provided for potential future direct calls.
        logger.info("🌙 Completing purchase - Order: {}, Lúa: {}", orderCode, luaAmount);
        // The actual crediting is done in PaymentServiceImpl.handleWebhook()
    }

    @Override
    public Page<CreditHistoryDTO> getTransactionHistory(UUID userId, String type, Pageable pageable) {
        logger.debug("📜 Getting transaction history - User: {}, Type: {}", userId, type);

        Page<CreditTransaction> transactions;

        if (type == null || type.equalsIgnoreCase("all")) {
            transactions = transactionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        } else if (type.equalsIgnoreCase("earn")) {
            transactions = transactionRepository.findByUserIdAndTypeOrderByCreatedAtDesc(
                    userId, CreditTransaction.Type.EARN, pageable);
        } else if (type.equalsIgnoreCase("spend")) {
            transactions = transactionRepository.findByUserIdAndTypeOrderByCreatedAtDesc(
                    userId, CreditTransaction.Type.SPEND, pageable);
        } else {
            // Default to all for invalid type
            transactions = transactionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        }

        return transactions.map(CreditHistoryDTO::fromEntity);
    }
}
