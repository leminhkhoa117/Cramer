package com.cramer.catalog.web;

import com.cramer.platform.common.ielts.Skill;
import com.cramer.catalog.service.TestDeliveryService;
import com.cramer.catalog.web.dto.TestSectionView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Test-delivery endpoint (SPEC-11 §2). Returns answer-free section/question views for the
 * test-taking UI. The old answer-bearing generic reads ({@code /api/questions/**},
 * {@code /api/sections/{id}/full}) are removed (SPEC-11 §2, SPEC-04 §3).
 */
@RestController
@RequestMapping("/api/tests")
public class TestDeliveryController {

    private final TestDeliveryService delivery;

    public TestDeliveryController(TestDeliveryService delivery) {
        this.delivery = delivery;
    }

    @GetMapping("/data")
    public List<TestSectionView> getTestData(@RequestParam("source") String source,
                                             @RequestParam("test") int test,
                                             @RequestParam("skill") String skill) {
        return delivery.getTestData(source, test, Skill.from(skill));
    }
}
