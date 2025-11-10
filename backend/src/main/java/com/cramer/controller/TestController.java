package com.cramer.controller;

import com.cramer.dto.FullSectionDTO;
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
    @Operation(summary = "Get full data for a test section (passages and questions)")
    public ResponseEntity<List<FullSectionDTO>> getFullTest(
            @RequestParam String source,
            @RequestParam Integer test,
            @RequestParam String skill) {
        
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TestController.class);
        logger.info("📥 GET /api/tests/data - source={}, test={}, skill={}", source, test, skill);
        
        try {
            List<FullSectionDTO> fullTest = testService.getFullTest(source, test, skill);
            
            if (fullTest == null || fullTest.isEmpty()) {
                logger.warn("⚠️ No test data found for source={}, test={}, skill={}", source, test, skill);
                return ResponseEntity.notFound().build();
            }
            
            logger.info("✅ Returning {} sections", fullTest.size());
            return ResponseEntity.ok(fullTest);
        } catch (IllegalArgumentException e) {
            logger.error("❌ Invalid parameters: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("❌ Error fetching test data: source={}, test={}, skill={}", source, test, skill, e);
            throw e;
        }
    }
}
