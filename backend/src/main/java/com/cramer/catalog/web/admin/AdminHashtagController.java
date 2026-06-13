package com.cramer.catalog.web.admin;

import com.cramer.catalog.service.HashtagService;
import com.cramer.catalog.web.dto.HashtagRequest;
import com.cramer.catalog.web.dto.HashtagView;
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
 * Admin hashtag CRUD + queries (SPEC-11 §4). Soft delete; lists return active only.
 */
@RestController
@RequestMapping("/api/admin/hashtags")
public class AdminHashtagController {

    private final HashtagService service;

    public AdminHashtagController(HashtagService service) {
        this.service = service;
    }

    @GetMapping
    public List<HashtagView> list() {
        return service.listActive();
    }

    @GetMapping("/category/{category}")
    public List<HashtagView> byCategory(@PathVariable String category) {
        return service.byCategory(category);
    }

    @GetMapping("/search")
    public List<HashtagView> search(@RequestParam("q") String query) {
        return service.search(query);
    }

    @GetMapping("/popular")
    public List<HashtagView> popular(@RequestParam(defaultValue = "10") int limit) {
        return service.popular(limit);
    }

    @GetMapping("/categories")
    public List<String> categories() {
        return service.categories();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HashtagView create(@Valid @RequestBody HashtagRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public HashtagView update(@PathVariable Long id, @Valid @RequestBody HashtagRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.softDelete(id);
    }
}
