package com.team.corporate.backend.controllers;

import com.team.corporate.backend.dto.CreateTagRequest;
import com.team.corporate.backend.dto.TagResponse;
import com.team.corporate.backend.dto.UpdateTagRequest;
import com.team.corporate.backend.services.TagsService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tags")
public class TagsController {
    private final TagsService tags;

    public TagsController(TagsService tags) {
        this.tags = tags;
    }

    @GetMapping
    public List<TagResponse> findAll() {
        return tags.findAll();
    }

    @GetMapping("/{id}")
    public TagResponse getTag(@PathVariable("id") UUID id) {
        return tags.getById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<TagResponse> createTag(@Valid @RequestBody CreateTagRequest request) {
        var tag = tags.create(request.name());
        return ResponseEntity.created(URI.create("/api/tags/" + tag.id())).body(tag);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public TagResponse updateTag(@PathVariable("id") UUID id, @Valid @RequestBody UpdateTagRequest request) {
        return tags.update(id, request.name());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTag(@PathVariable("id") UUID id) {
        tags.delete(id);
    }
}
