package com.cramer.catalog.web.admin;

import com.cramer.catalog.service.QuestionService;
import com.cramer.catalog.web.dto.QuestionAdminView;
import com.cramer.catalog.web.dto.QuestionRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin question CRUD (SPEC-11 §4). Returns the answer key + explanation (admin surface only).
 */
@RestController
@RequestMapping("/api/admin/questions")
public class AdminQuestionController {

    private final QuestionService service;

    public AdminQuestionController(QuestionService service) {
        this.service = service;
    }

    @GetMapping
    public List<QuestionAdminView> listBySection(@RequestParam Long sectionId) {
        return service.listBySection(sectionId);
    }

    @GetMapping("/{id}")
    public QuestionAdminView get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public QuestionAdminView create(@Valid @RequestBody QuestionRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public QuestionAdminView update(@PathVariable Long id, @Valid @RequestBody QuestionRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
