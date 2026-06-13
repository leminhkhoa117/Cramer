package com.cramer.admin.web;

import com.cramer.admin.service.AdminFinanceService;
import com.cramer.admin.web.dto.AdminDtos;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin finance reporting (SPEC-17 §5). All figures derive from {@code payment_orders.status='PAID'}
 * + {@code amount_vnd}. Read-only projections.
 */
@RestController
@RequestMapping("/api/admin/finance")
public class AdminFinanceController {

    private final AdminFinanceService finance;

    public AdminFinanceController(AdminFinanceService finance) {
        this.finance = finance;
    }

    @GetMapping("/overview")
    public AdminDtos.FinanceOverviewView overview(@RequestParam(defaultValue = "30d") String period) {
        return finance.overview(period);
    }

    @GetMapping("/breakdown")
    public AdminDtos.RevenueBreakdownView breakdown(@RequestParam(defaultValue = "30d") String period) {
        return finance.breakdown(period);
    }

    @GetMapping("/top-spenders")
    public List<AdminDtos.TopSpenderView> topSpenders(@RequestParam(defaultValue = "10") int limit) {
        return finance.topSpenders(limit);
    }

    @GetMapping("/transactions")
    public List<AdminDtos.AuditLogView> transactions(@RequestParam(required = false) String status,
                                                     @RequestParam(required = false) String type,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "20") int size) {
        return finance.transactions(status, type, page, size);
    }
}
