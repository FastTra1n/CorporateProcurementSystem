package com.team.corporate.backend.controllers;

import com.team.corporate.backend.dto.CreateUserRequest;
import com.team.corporate.backend.dto.PageResponse;
import com.team.corporate.backend.dto.UpdateUserRequest;
import com.team.corporate.backend.dto.AccountStatusRequest;
import com.team.corporate.backend.dto.ResetPasswordRequest;
import com.team.corporate.backend.dto.UserResponse;
import com.team.corporate.backend.services.AccountsService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
public class UsersController {
    private final AccountsService accounts;

    public UsersController(AccountsService accounts) {
        this.accounts = accounts;
    }

    @GetMapping
    public PageResponse<UserResponse> getUsers(
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(AccountsService.MAX_PAGE_SIZE) int size
    ) {
        return accounts.getUsers(page, size);
    }

    @GetMapping("/{id}")
    public UserResponse getUser(@PathVariable("id") UUID id) {
        return accounts.getUser(id);
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserResponse user = accounts.createUser(request);
        return ResponseEntity.created(URI.create("/api/admin/users/" + user.id())).body(user);
    }

    @PutMapping("/{id}")
    public UserResponse updateUser(@PathVariable("id") UUID id, @Valid @RequestBody UpdateUserRequest request) {
        return accounts.updateUser(id, request);
    }

    @PatchMapping("/{id}/status")
    public UserResponse setStatus(@PathVariable("id") UUID id, @Valid @RequestBody AccountStatusRequest request) {
        return accounts.setEnabled(id, request.enabled());
    }

    @PutMapping("/{id}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@PathVariable("id") UUID id, @Valid @RequestBody ResetPasswordRequest request) {
        accounts.resetPassword(id, request.password());
    }
}
