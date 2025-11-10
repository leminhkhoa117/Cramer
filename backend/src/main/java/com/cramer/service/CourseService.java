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

    public List<String> getCourses() {
        return sectionRepository.findDistinctExamSources();
    }

    public List<Integer> getTestsForCourse(String courseName) {
        return sectionRepository.findDistinctTestNumbersByExamSource(courseName);
    }
}
