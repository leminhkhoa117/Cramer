package com.cramer.service;

import com.cramer.dto.FullSectionDTO;
import com.cramer.dto.TestSectionDTO;
import com.cramer.entity.Question;
import com.cramer.entity.Section;
import com.cramer.mapper.TestMapper;
import com.cramer.repository.QuestionRepository;
import com.cramer.repository.SectionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TestService {

    private final SectionRepository sectionRepository;
    private final QuestionRepository questionRepository;

    @Autowired
    public TestService(SectionRepository sectionRepository, QuestionRepository questionRepository) {
        this.sectionRepository = sectionRepository;
        this.questionRepository = questionRepository;
    }

    /**
     * Get full test data INCLUDING answers (for admin/debugging only).
     */
    @Transactional(readOnly = true)
    public List<FullSectionDTO> getFullTest(String source, Integer testNum, String skill) {
        return getTestData(source, testNum, skill, true);
    }

    /**
     * Get safe test data EXCLUDING answers (for test taking).
     */
    @Transactional(readOnly = true)
    public List<TestSectionDTO> getSafeTest(String source, Integer testNum, String skill) {
        List<FullSectionDTO> fullData = getTestData(source, testNum, skill, false);
        
        // Convert FullSectionDTO to TestSectionDTO
        return fullData.stream().map(full -> {
            return new TestSectionDTO(
                full, // FullSectionDTO extends SectionDTO so this works
                full.getQuestions().stream().map(q -> new com.cramer.dto.TestQuestionDTO(
                    q.getId(),
                    q.getSectionId(),
                    q.getQuestionNumber(),
                    q.getQuestionUid(),
                    q.getQuestionType(),
                    q.getQuestionContent(),
                    q.getWordLimit(),
                    q.getImageUrl()
                )).collect(Collectors.toList())
            );
        }).collect(Collectors.toList());
    }

    private List<FullSectionDTO> getTestData(String source, Integer testNum, String skill, boolean includeAnswers) {
        try {
            // Validate inputs
            if (source == null || source.trim().isEmpty()) {
                throw new IllegalArgumentException("Source cannot be null or empty");
            }
            if (testNum == null || testNum < 1) {
                throw new IllegalArgumentException("Test number must be greater than 0");
            }
            if (skill == null || skill.trim().isEmpty()) {
                throw new IllegalArgumentException("Skill cannot be null or empty");
            }
            
            org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TestService.class);
            logger.info("🔍 Fetching test data: source={}, testNum={}, skill={}", source, testNum, skill);
            
            // 1. Fetch all sections (passages) for the given test
            List<Section> sections = sectionRepository.findSectionsForTest(source, testNum, skill);
            logger.info("📋 Found {} sections for test", sections.size());
            
            if (sections.isEmpty()) {
                logger.warn("⚠️ No sections found for source={}, testNum={}, skill={}", source, testNum, skill);
                return List.of();
            }

            // 2. For each section, fetch its questions and map to DTO
            List<FullSectionDTO> result = sections.stream().map(section -> {
                try {
                    logger.info("📝 Processing section id={}, part={}", section.getId(), section.getPartNumber());
                    
                    // Fetch questions for the current section
                    List<Question> questions = questionRepository.findBySectionId(section.getId());
                    logger.info("   ✓ Found {} questions for section {}", questions.size(), section.getId());
                    
                    // Map the section and its questions to a FullSectionDTO
                    return TestMapper.toFullSectionDTO(section, questions);
                } catch (Exception e) {
                    logger.error("❌ Error processing section id={}: {}", section.getId(), e.getMessage(), e);
                    throw new RuntimeException("Failed to process section " + section.getId(), e);
                }
            }).collect(Collectors.toList());
            
            logger.info("✅ Successfully built {} section DTOs", result.size());
            return result;
            
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TestService.class);
            logger.error("❌ Fatal error in getTestData: source={}, testNum={}, skill={}", source, testNum, skill, e);
            throw new RuntimeException("Failed to fetch test data: " + e.getMessage(), e);
        }
    }
}
