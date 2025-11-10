package com.cramer.service;

import com.cramer.dto.FullSectionDTO;
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

    @Transactional(readOnly = true)
    public List<FullSectionDTO> getFullTest(String source, Integer testNum, String skill) {
        // 1. Fetch all sections (passages) for the given test
        List<Section> sections = sectionRepository.findSectionsForTest(source, testNum, skill);

        // 2. For each section, fetch its questions and map to DTO
        return sections.stream().map(section -> {
            // Fetch questions for the current section
            List<Question> questions = questionRepository.findBySectionId(section.getId());
            
            // Map the section and its questions to a FullSectionDTO
            return TestMapper.toFullSectionDTO(section, questions);
        }).collect(Collectors.toList());
    }
}
