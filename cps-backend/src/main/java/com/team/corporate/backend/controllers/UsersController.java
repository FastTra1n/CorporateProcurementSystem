package com.team.corporate.backend.controllers;

import com.team.corporate.backend.dto.CreateUserRequest;
import com.team.corporate.backend.dto.PageResponse;
import com.team.corporate.backend.dto.UpdateUserRequest;
import com.team.corporate.backend.dto.AccountStatusRequest;
import com.team.corporate.backend.dto.ResetPasswordRequest;
import com.team.corporate.backend.dto.UserResponse;
import com.team.corporate.backend.services.AccountsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Пользователи", description = "Только ADMIN после обязательной смены временного пароля.")
public class UsersController {
    private final AccountsService accounts;

    public UsersController(AccountsService accounts) {
        this.accounts = accounts;
    }

    @GetMapping
    @Operation(summary = "Получить страницу пользователей", description = "Нумерация страниц с 0; size от 1 до 100. Ответ содержит content, page, size, totalElements и totalPages.")
    public PageResponse<UserResponse> getUsers(
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(AccountsService.MAX_PAGE_SIZE) int size
    ) {
        return accounts.getUsers(page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить пользователя по UUID")
    public UserResponse getUser(@PathVariable("id") UUID id) {
        return accounts.getUser(id);
    }

    @PostMapping
    @Operation(summary = "Создать пользователя с временным паролем", description = "Логин уникален. После первого входа пользователь обязан сменить пароль. Конфликт логина — 409.")
    @ApiResponse(responseCode = "201", description = "Пользователь создан")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserResponse user = accounts.createUser(request);
        return ResponseEntity.created(URI.create("/api/admin/users/" + user.id())).body(user);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Изменить пользователя", description = "Обязательны username, fullName, role и enabled. Изменение логина, роли или активности отзывает все сессии; изменение только имени сохраняет их.")
    public UserResponse updateUser(@PathVariable("id") UUID id, @Valid @RequestBody UpdateUserRequest request) {
        return accounts.updateUser(id, request);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Включить или отключить учётную запись")
    public UserResponse setStatus(@PathVariable("id") UUID id, @Valid @RequestBody AccountStatusRequest request) {
        return accounts.setEnabled(id, request.enabled());
    }

    @PutMapping("/{id}/password")
    @Operation(summary = "Задать пользователю временный пароль", description = "Отзывает все сессии и требует сменить пароль при следующем входе.")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@PathVariable("id") UUID id, @Valid @RequestBody ResetPasswordRequest request) {
        accounts.resetPassword(id, request.password());
    }
}
