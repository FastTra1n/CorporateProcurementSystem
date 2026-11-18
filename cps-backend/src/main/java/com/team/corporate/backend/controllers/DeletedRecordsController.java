package com.team.corporate.backend.controllers;

import com.team.corporate.backend.services.SoftDeleteAdministrationService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/deleted")
@PreAuthorize("hasRole('ADMIN')")
public class DeletedRecordsController {
    private final SoftDeleteAdministrationService administration;

    public DeletedRecordsController(SoftDeleteAdministrationService administration) {
        this.administration = administration;
    }

    @PostMapping("/{entity}/{id}/restore")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void restore(@PathVariable("entity") String entity, @PathVariable("id") UUID id) {
        administration.restore(entity, id);
    }

    @DeleteMapping("/{entity}/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void purge(@PathVariable("entity") String entity, @PathVariable("id") UUID id) {
        administration.purge(entity, id);
    }
}
