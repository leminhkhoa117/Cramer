package com.cramer.catalog.web.admin;

import com.cramer.catalog.service.TestSetService;
import com.cramer.catalog.web.dto.CreateTestSetRequest;
import com.cramer.catalog.web.dto.ReorderRequest;
import com.cramer.catalog.web.dto.TestSetView;
import com.cramer.platform.security.CurrentUser;
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

import java.util.List;

/**
 * Admin test-set CRUD + publish + reorder (SPEC-11 §4). Admin-gated via {@code /api/admin/**}
 * (SPEC-04 §1).
 */
@RestController
@RequestMapping("/api/admin/test-sets")
public class AdminTestSetController {

    private final TestSetService service;
    private final CurrentUser currentUser;

    public AdminTestSetController(TestSetService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<TestSetView> list() {
        return service.listAll();
    }

    @GetMapping("/{id}")
    public TestSetView get(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping("/code/{code}")
    public TestSetView getByCode(@PathVariable String code) {
        return service.getByCode(code);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TestSetView create(@Valid @RequestBody CreateTestSetRequest request) {
        return service.create(request, currentUser.requireUserId());
    }

    @PutMapping("/{id}")
    public TestSetView update(@PathVariable Long id, @Valid @RequestBody CreateTestSetRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @PostMapping("/{id}/publish")
    public TestSetView publish(@PathVariable Long id) {
        return service.publish(id, true);
    }

    @PostMapping("/{id}/unpublish")
    public TestSetView unpublish(@PathVariable Long id) {
        return service.publish(id, false);
    }

    @PostMapping("/reorder")
    public List<TestSetView> reorder(@Valid @RequestBody ReorderRequest request) {
        service.reorder(request.orderedIds());
        return service.listAll();
    }
}
