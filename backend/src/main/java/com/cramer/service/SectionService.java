package com.cramer.service;

import com.cramer.dto.FullSectionDTO;
import com.cramer.dto.QuestionDTO;
import com.cramer.entity.Section;
import com.cramer.exception.ResourceNotFoundException;
import com.cramer.repository.SectionRepository;
import com.cramer.util.EntityMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service layer for Section entity.
 * Handles business logic for exam section management.
 */
@Service
@Transactional
public class SectionService {

    private static final Logger logger = LoggerFactory.getLogger(SectionService.class);

    private final SectionRepository sectionRepository;
    private final QuestionService questionService;

    @Autowired
    public SectionService(SectionRepository sectionRepository, QuestionService questionService) {
        this.sectionRepository = sectionRepository;
        this.questionService = questionService;
    }

    /**
     * Get a full section with all its questions.
     *
     * @param id the section ID
     * @return DTO containing the section and its questions
     * @throws ResourceNotFoundException if section not found
     */
    @Transactional(readOnly = true)
    public FullSectionDTO getFullSectionById(Long id) {
        logger.info("Fetching full section by ID: {}", id);

        // 1. Fetch the section entity
        Section section = sectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Section", "id", id));

        // 2. Fetch the associated questions
        List<QuestionDTO> questions = questionService.getQuestionsBySectionId(id)
                .stream()
                .map(EntityMapper::toDTO)
                .collect(Collectors.toList());

        // 3. Combine them into a FullSectionDTO
        FullSectionDTO fullSectionDTO = new FullSectionDTO(EntityMapper.toDTO(section), questions);

        return fullSectionDTO;
    }

    /**
     * Get all sections.
     * 
     * @return list of all sections
     */
    @Transactional(readOnly = true)
    public List<Section> getAllSections() {
        logger.info("Fetching all sections");
        return sectionRepository.findAll();
    }

    /**
     * Get section by ID.
     * 
     * @param id the section ID
     * @return Optional containing the section if found
     */
    @Transactional(readOnly = true)
    public Optional<Section> getSectionById(Long id) {
        logger.info("Fetching section by ID: {}", id);
        return sectionRepository.findById(id);
    }

    /**
     * Get sections by exam source.
     * 
     * @param examSource the exam source (e.g., "cam17")
     * @return list of sections
     */
    @Transactional(readOnly = true)
    public List<Section> getSectionsByExamSource(String examSource) {
        logger.info("Fetching sections by exam source: {}", examSource);
        return sectionRepository.findByExamSource(examSource);
    }

    /**
     * Get sections for a specific test.
     * 
     * @param examSource the exam source
     * @param testNumber the test number
     * @return list of sections
     */
    @Transactional(readOnly = true)
    public List<Section> getSectionsByTest(String examSource, Integer testNumber) {
        logger.info("Fetching sections for exam: {}, test: {}", examSource, testNumber);
        return sectionRepository.findByExamSourceAndTestNumber(examSource, testNumber);
    }

    /**
     * Get sections by skill type.
     * 
     * @param skill the skill (e.g., "reading", "listening")
     * @return list of sections
     */
    @Transactional(readOnly = true)
    public List<Section> getSectionsBySkill(String skill) {
        logger.info("Fetching sections by skill: {}", skill);
        return sectionRepository.findBySkill(skill);
    }

    /**
     * Get a specific section by all identifying parameters.
     * 
     * @param examSource the exam source
     * @param testNumber the test number
     * @param skill the skill type
     * @param partNumber the part number
     * @return Optional containing the section if found
     */
    @Transactional(readOnly = true)
    public Optional<Section> getSpecificSection(String examSource, Integer testNumber, 
                                                String skill, Integer partNumber) {
        logger.info("Fetching specific section: {}-T{}-{}-P{}", examSource, testNumber, skill, partNumber);
        return sectionRepository.findByExamSourceAndTestNumberAndSkillAndPartNumber(
                examSource, testNumber, skill, partNumber);
    }

    /**
     * Get sections for a test and skill, ordered by part number.
     * 
     * @param examSource the exam source
     * @param testNumber the test number
     * @param skill the skill type
     * @return list of sections ordered by part
     */
    @Transactional(readOnly = true)
    public List<Section> getSectionsForTest(String examSource, Integer testNumber, String skill) {
        logger.info("Fetching ordered sections for: {}-T{}-{}", examSource, testNumber, skill);
        return sectionRepository.findSectionsForTest(examSource, testNumber, skill);
    }

    /**
     * Create a new section.
     * 
     * @param section the section to create
     * @return the created section
     * @throws IllegalArgumentException if section already exists
     */
    public Section createSection(Section section) {
        logger.info("Creating new section: {}-T{}-{}-P{}", 
                section.getExamSource(), section.getTestNumber(), 
                section.getSkill(), section.getPartNumber());
        
        if (sectionRepository.existsByExamSourceAndTestNumberAndSkillAndPartNumber(
                section.getExamSource(), section.getTestNumber(), 
                section.getSkill(), section.getPartNumber())) {
            logger.error("Section already exists");
            throw new IllegalArgumentException("Section already exists with these parameters");
        }
        
        Section savedSection = sectionRepository.save(section);
        logger.info("Section created successfully with ID: {}", savedSection.getId());
        return savedSection;
    }

    /**
     * Update an existing section.
     * 
     * @param id the section ID
     * @param updatedSection the updated section data
     * @return the updated section
     * @throws IllegalArgumentException if section not found
     */
    public Section updateSection(Long id, Section updatedSection) {
        logger.info("Updating section with ID: {}", id);
        
        Section existingSection = sectionRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Section not found with ID: {}", id);
                    return new IllegalArgumentException("Section not found with ID: " + id);
                });
        
        existingSection.setExamSource(updatedSection.getExamSource());
        existingSection.setTestNumber(updatedSection.getTestNumber());
        existingSection.setSkill(updatedSection.getSkill());
        existingSection.setPartNumber(updatedSection.getPartNumber());
        existingSection.setDisplayContentUrl(updatedSection.getDisplayContentUrl());
        existingSection.setPassageText(updatedSection.getPassageText());
        
        Section savedSection = sectionRepository.save(existingSection);
        logger.info("Section updated successfully: {}", id);
        return savedSection;
    }

    /**
     * Delete a section by ID.
     * 
     * @param id the section ID
     * @throws IllegalArgumentException if section not found
     */
    public void deleteSection(Long id) {
        logger.info("Deleting section with ID: {}", id);
        
        if (!sectionRepository.existsById(id)) {
            logger.error("Section not found with ID: {}", id);
            throw new IllegalArgumentException("Section not found with ID: " + id);
        }
        
        sectionRepository.deleteById(id);
        logger.info("Section deleted successfully: {}", id);
    }

    /**
     * Count sections by exam source.
     * 
     * @param examSource the exam source
     * @return number of sections
     */
    @Transactional(readOnly = true)
    public long countByExamSource(String examSource) {
        logger.info("Counting sections for exam source: {}", examSource);
        return sectionRepository.countByExamSource(examSource);
    }

    /**
     * Get total section count.
     * 
     * @return total number of sections
     */
    @Transactional(readOnly = true)
    public long getTotalSectionCount() {
        logger.info("Getting total section count");
        return sectionRepository.count();
    }
}
