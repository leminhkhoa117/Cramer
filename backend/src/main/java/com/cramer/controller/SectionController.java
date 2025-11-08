package com.cramer.controller;

import com.cramer.dto.FullSectionDTO;
import com.cramer.dto.SectionDTO;
import com.cramer.entity.Section;
import com.cramer.exception.ResourceNotFoundException;
import com.cramer.service.SectionService;
import com.cramer.util.EntityMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST Controller for Section management.
 * Provides CRUD operations and queries for exam sections.
 */
@RestController
@RequestMapping("/api/sections")
@CrossOrigin(origins = "*")
public class SectionController {

    private static final Logger logger = LoggerFactory.getLogger(SectionController.class);

    private final SectionService sectionService;

    @Autowired
    public SectionController(SectionService sectionService) {
        this.sectionService = sectionService;
    }

    /**
     * Get all sections.
     * GET /api/sections
     */
    @GetMapping
    public ResponseEntity<List<SectionDTO>> getAllSections() {
        logger.info("REST request to get all sections");
        List<SectionDTO> sections = sectionService.getAllSections()
                .stream()
                .map(EntityMapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(sections);
    }

    /**
     * Get a full section, including its questions.
     * GET /api/sections/{id}/full
     */
    @GetMapping("/{id}/full")
    public ResponseEntity<FullSectionDTO> getFullSectionById(@PathVariable Long id) {
        logger.info("REST request to get full section by ID: {}", id);
        FullSectionDTO fullSection = sectionService.getFullSectionById(id);
        return ResponseEntity.ok(fullSection);
    }

    /**
     * Get section by ID.
     * GET /api/sections/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<SectionDTO> getSectionById(@PathVariable Long id) {
        logger.info("REST request to get section by ID: {}", id);
        Section section = sectionService.getSectionById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Section", "id", id));
        return ResponseEntity.ok(EntityMapper.toDTO(section));
    }

    /**
     * Get sections by exam source.
     * GET /api/sections/exam/{examSource}
     */
    @GetMapping("/exam/{examSource}")
    public ResponseEntity<List<SectionDTO>> getSectionsByExamSource(@PathVariable String examSource) {
        logger.info("REST request to get sections by exam source: {}", examSource);
        List<SectionDTO> sections = sectionService.getSectionsByExamSource(examSource)
                .stream()
                .map(EntityMapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(sections);
    }

    /**
     * Get sections by exam source and test number.
     * GET /api/sections/exam/{examSource}/test/{testNumber}
     */
    @GetMapping("/exam/{examSource}/test/{testNumber}")
    public ResponseEntity<List<SectionDTO>> getSectionsByTest(
            @PathVariable String examSource,
            @PathVariable Integer testNumber) {
        logger.info("REST request to get sections for exam: {}, test: {}", examSource, testNumber);
        List<SectionDTO> sections = sectionService.getSectionsByTest(examSource, testNumber)
                .stream()
                .map(EntityMapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(sections);
    }

    /**
     * Get sections by skill.
     * GET /api/sections/skill/{skill}
     */
    @GetMapping("/skill/{skill}")
    public ResponseEntity<List<SectionDTO>> getSectionsBySkill(@PathVariable String skill) {
        logger.info("REST request to get sections by skill: {}", skill);
        List<SectionDTO> sections = sectionService.getSectionsBySkill(skill)
                .stream()
                .map(EntityMapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(sections);
    }

    /**
     * Get specific section by all parameters.
     * GET /api/sections/specific
     */
    @GetMapping("/specific")
    public ResponseEntity<SectionDTO> getSpecificSection(
            @RequestParam String examSource,
            @RequestParam Integer testNumber,
            @RequestParam String skill,
            @RequestParam Integer partNumber) {
        logger.info("REST request to get specific section: {}-T{}-{}-P{}", 
                examSource, testNumber, skill, partNumber);
        Section section = sectionService.getSpecificSection(examSource, testNumber, skill, partNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Section not found: %s-T%d-%s-P%d", 
                                examSource, testNumber, skill, partNumber)));
        return ResponseEntity.ok(EntityMapper.toDTO(section));
    }

    /**
     * Get sections for test and skill (ordered).
     * GET /api/sections/exam/{examSource}/test/{testNumber}/skill/{skill}
     */
    @GetMapping("/exam/{examSource}/test/{testNumber}/skill/{skill}")
    public ResponseEntity<List<SectionDTO>> getSectionsForTest(
            @PathVariable String examSource,
            @PathVariable Integer testNumber,
            @PathVariable String skill) {
        logger.info("REST request to get ordered sections for: {}-T{}-{}", examSource, testNumber, skill);
        List<SectionDTO> sections = sectionService.getSectionsForTest(examSource, testNumber, skill)
                .stream()
                .map(EntityMapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(sections);
    }

    /**
     * Create a new section.
     * POST /api/sections
     */
    @PostMapping
    public ResponseEntity<SectionDTO> createSection(@RequestBody SectionDTO sectionDTO) {
        logger.info("REST request to create section");
        Section section = EntityMapper.toEntity(sectionDTO);
        Section createdSection = sectionService.createSection(section);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(EntityMapper.toDTO(createdSection));
    }

    /**
     * Update an existing section.
     * PUT /api/sections/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<SectionDTO> updateSection(
            @PathVariable Long id,
            @RequestBody SectionDTO sectionDTO) {
        logger.info("REST request to update section: {}", id);
        Section section = EntityMapper.toEntity(sectionDTO);
        Section updatedSection = sectionService.updateSection(id, section);
        return ResponseEntity.ok(EntityMapper.toDTO(updatedSection));
    }

    /**
     * Delete a section.
     * DELETE /api/sections/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSection(@PathVariable Long id) {
        logger.info("REST request to delete section: {}", id);
        sectionService.deleteSection(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Count sections by exam source.
     * GET /api/sections/count/exam/{examSource}
     */
    @GetMapping("/count/exam/{examSource}")
    public ResponseEntity<Long> countByExamSource(@PathVariable String examSource) {
        logger.info("REST request to count sections for exam: {}", examSource);
        long count = sectionService.countByExamSource(examSource);
        return ResponseEntity.ok(count);
    }

    /**
     * Get total section count.
     * GET /api/sections/count
     */
    @GetMapping("/count")
    public ResponseEntity<Long> getSectionCount() {
        logger.info("REST request to get section count");
        long count = sectionService.getTotalSectionCount();
        return ResponseEntity.ok(count);
    }
}
