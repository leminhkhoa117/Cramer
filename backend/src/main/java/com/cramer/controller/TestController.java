package com.cramer.controller;

import com.cramer.dto.TestSectionDTO;
import com.cramer.service.TestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tests")
@Tag(name = "Tests API", description = "API for fetching test data")
public class TestController {

    private final TestService testService;

    @Autowired
    public TestController(TestService testService) {
        this.testService = testService;
    }

    @GetMapping("/data")
    @Operation(summary = "Get SAFE data for a test section (passages and questions WITHOUT answers)")
    public ResponseEntity<List<TestSectionDTO>> getFullTest(
            @RequestParam String source,
            @RequestParam Integer test,
            @RequestParam String skill) {
        
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TestController.class);
        logger.info("📥 GET /api/tests/data - source={}, test={}, skill={}", source, test, skill);
        
        try {
            // Use getSafeTest to return DTOs without answers
            List<TestSectionDTO> safeTest = testService.getSafeTest(source, test, skill);
            
            if (safeTest == null || safeTest.isEmpty()) {
                logger.warn("⚠️ No test data found for source={}, test={}, skill={}", source, test, skill);
                return ResponseEntity.notFound().build();
            }
            
            logger.info("✅ Returning {} sections (SAFE MODE)", safeTest.size());
            return ResponseEntity.ok(safeTest);
        } catch (IllegalArgumentException e) {
            logger.error("❌ Invalid parameters: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("❌ Error fetching test data: source={}, test={}, skill={}", source, test, skill, e);
            throw e;
        }
    }
}
