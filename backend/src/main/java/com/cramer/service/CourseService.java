package com.cramer.service;

import com.cramer.repository.SectionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CourseService {

    private final SectionRepository sectionRepository;

    @Autowired
    public CourseService(SectionRepository sectionRepository) {
        this.sectionRepository = sectionRepository;
    }

    public com.cramer.dto.PageDTO<String> getCourses(int page, int size, String search) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        org.springframework.data.domain.Page<String> coursePage = sectionRepository.findDistinctExamSources(pageable, search);
        
        return new com.cramer.dto.PageDTO<>(
            coursePage.getContent(),
            coursePage.getNumber(),
            coursePage.getSize(),
            coursePage.getTotalElements(),
            coursePage.getTotalPages()
        );
    }

    public List<Integer> getTestsForCourse(String courseName) {
        return sectionRepository.findDistinctTestNumbersByExamSource(courseName);
    }
}
