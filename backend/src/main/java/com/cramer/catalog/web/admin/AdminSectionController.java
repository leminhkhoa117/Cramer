package com.cramer.catalog.web.admin;

import com.cramer.catalog.service.SectionService;
import com.cramer.catalog.web.dto.SectionAdminView;
import com.cramer.catalog.web.dto.SectionRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin section CRUD (SPEC-11 §4). Replaces the old generic, answer-leaking section endpoints.
 */
@RestController
@RequestMapping("/api/admin/sections")
public class AdminSectionController {

    private final SectionService service;

    public AdminSectionController(SectionService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    public SectionAdminView get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SectionAdminView create(@Valid @RequestBody SectionRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public SectionAdminView update(@PathVariable Long id, @Valid @RequestBody SectionRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
