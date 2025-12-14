package com.cramer.service;

import com.cramer.dto.CreditHistoryDTO;
import com.cramer.dto.LuaPurchaseResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for Lúa credit package operations.
 * Handles package listing, purchase initiation, and transaction history.
 */
public interface LuaCreditService {

    /**
     * Represents a Lúa package available for purchase.
     */
    record LuaPackage(
            String code,
            String name,
            int luaAmount,
            int priceVnd,
            int bonusPercent
    ) {
        /**
         * Calculate bonus Lúa amount.
         */
        public int getBonusAmount() {
            return (luaAmount * bonusPercent) / 100;
        }

        /**
         * Get total Lúa (base + bonus).
         */
        public int getTotalLua() {
            return luaAmount + getBonusAmount();
        }

        /**
         * Get price per Lúa (VND).
         */
        public double getPricePerLua() {
            return (double) priceVnd / getTotalLua();
        }
    }

    /**
     * Get all available Lúa packages.
     *
     * @return list of available packages
     */
    List<LuaPackage> getAvailablePackages();

    /**
     * Get a specific package by code.
     *
     * @param packageCode the package code (small, medium, large)
     * @return the package or null if not found
     */
    LuaPackage getPackageByCode(String packageCode);

    /**
     * Initiate a Lúa package purchase.
     * Creates a PayOS payment link for the user.
     *
     * @param userId      the user's UUID
     * @param packageCode the package code to purchase
     * @return response with checkout URL or error message
     */
    LuaPurchaseResponseDTO initiatePurchase(UUID userId, String packageCode);

    /**
     * Complete a Lúa purchase after payment confirmation.
     * Called by the payment webhook handler.
     *
     * @param orderCode the PayOS order code
     * @param luaAmount the Lúa amount to credit
     */
    void completePurchase(Long orderCode, int luaAmount);

    /**
     * Get transaction history with optional type filter.
     *
     * @param userId   the user's UUID
     * @param type     filter type: "all", "earn", or "spend" (null = all)
     * @param pageable pagination parameters
     * @return page of transaction history DTOs
     */
    Page<CreditHistoryDTO> getTransactionHistory(UUID userId, String type, Pageable pageable);
}
