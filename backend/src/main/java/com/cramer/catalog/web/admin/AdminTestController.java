package com.cramer.catalog.web.admin;

import com.cramer.catalog.service.TestAdminService;
import com.cramer.catalog.web.dto.CreateTestRequest;
import com.cramer.catalog.web.dto.SectionAdminView;
import com.cramer.catalog.web.dto.TestView;
import com.cramer.catalog.web.dto.UpdateTestHashtagsRequest;
import com.cramer.catalog.web.dto.UpdateTestRequest;
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
 * Admin test CRUD + publish cascade + duplicate + hashtags + section listing (SPEC-11 §4).
 */
@RestController
@RequestMapping("/api/admin")
public class AdminTestController {

    private final TestAdminService service;

    public AdminTestController(TestAdminService service) {
        this.service = service;
    }

    @GetMapping("/test-sets/{setId}/tests")
    public List<TestView> listBySet(@PathVariable Long setId) {
        return service.listBySet(setId);
    }

    @PostMapping("/test-sets/{setId}/tests")
    @ResponseStatus(HttpStatus.CREATED)
    public TestView create(@PathVariable Long setId, @Valid @RequestBody CreateTestRequest request) {
        return service.create(setId, request);
    }

    @GetMapping("/tests/lookup")
    public TestView lookup(@RequestParam String setCode, @RequestParam int testNumber) {
        return service.lookup(setCode, testNumber);
    }

    @GetMapping("/tests/{id}")
    public TestView get(@PathVariable Long id) {
        return service.getById(id);
    }

    @PutMapping("/tests/{id}")
    public TestView update(@PathVariable Long id, @Valid @RequestBody UpdateTestRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/tests/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean force) {
        service.delete(id, force);
    }

    @PostMapping("/tests/{id}/publish")
    public TestView publish(@PathVariable Long id) {
        return service.setPublished(id, true);
    }

    @PostMapping("/tests/{id}/unpublish")
    public TestView unpublish(@PathVariable Long id) {
        return service.setPublished(id, false);
    }

    @PostMapping("/tests/{id}/duplicate")
    @ResponseStatus(HttpStatus.CREATED)
    public TestView duplicate(@PathVariable Long id,
                              @RequestParam(defaultValue = "false") boolean includeSections) {
        return service.duplicate(id, includeSections);
    }

    @PutMapping("/tests/{id}/hashtags")
    public TestView replaceHashtags(@PathVariable Long id,
                                    @Valid @RequestBody UpdateTestHashtagsRequest request) {
        return service.replaceHashtags(id, request.codes());
    }

    @GetMapping("/tests/{id}/sections")
    public List<SectionAdminView> sectionsForTest(@PathVariable Long id,
                                                  @RequestParam(required = false) String skill) {
        return service.sectionsForTest(id, skill);
    }
}
