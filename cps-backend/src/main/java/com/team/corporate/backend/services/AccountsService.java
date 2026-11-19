package com.team.corporate.backend.services;

import com.team.corporate.backend.dto.ChangePasswordRequest;
import com.team.corporate.backend.audit.AuditEventWriter;
import com.team.corporate.backend.entities.AuditEntityType;
import com.team.corporate.backend.entities.AuditEventType;
import com.team.corporate.backend.dto.CreateUserRequest;
import com.team.corporate.backend.dto.PageResponse;
import com.team.corporate.backend.dto.UpdateUserRequest;
import com.team.corporate.backend.dto.UserResponse;
import com.team.corporate.backend.entities.UserCredentials;
import com.team.corporate.backend.repositories.SpringUsersRepository;
import com.team.corporate.backend.repositories.UserCredentialsRepository;
import com.team.corporate.core.entities.User;
import com.team.corporate.core.exceptions.UserNotFoundException;
import com.team.corporate.core.utils.EntityValidation;
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AccountsService {
    public static final int MAX_PAGE_SIZE = 100;

    private final SpringUsersRepository users;
    private final UserCredentialsRepository credentials;
    private final PasswordEncoder encoder;
    private final RefreshSessionsService sessions;
    private final AuditEventWriter audit;

    public AccountsService(
            @NotNull SpringUsersRepository users,
            @NotNull UserCredentialsRepository credentials,
            @NotNull PasswordEncoder encoder,
            @NotNull RefreshSessionsService sessions,
            @NotNull AuditEventWriter audit
    ) {
        this.users = users;
        this.credentials = credentials;
        this.encoder = encoder;
        this.sessions = sessions;
        this.audit = audit;
    }

    @NotNull
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public UserResponse createUser(@NotNull CreateUserRequest request) {
        User user = new User(request.username(), request.fullName(), request.role());
        if (users.findByUsername(user.getUsername()).isPresent()) {
            throw new DataIntegrityViolationException("Пользователь уже существует.");
        }
        users.save(user);
        credentials.saveAndFlush(new UserCredentials(user, encoder.encode(request.password())));
        return response(user);
    }

    @NotNull
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getUsers(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Номер страницы: от 0; размер страницы: от 1 до " + MAX_PAGE_SIZE + ".");
        }
        var allUsers = users.findAll(PageRequest.of(page, size, Sort.by("username", "id")));
        var passwordChanges = credentials.findAllById(allUsers.stream().map(User::getId).toList()).stream()
                .collect(Collectors.toMap(UserCredentials::getUserId, UserCredentials::isMustChangePassword));
        return PageResponse.of(allUsers.map(user ->
                UserResponse.of(user, passwordChanges.getOrDefault(user.getId(), false))));
    }

    @NotNull
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public UserResponse getUser(@NotNull UUID id) {
        return response(findUser(id));
    }

    @NotNull
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public UserResponse updateUser(@NotNull UUID id, @NotNull UpdateUserRequest request) {
        User user = users.findByIdForUpdate(id)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден."));
        String username = EntityValidation.requireText(request.username(), "Имя пользователя");
        users.findByUsername(username).filter(existing -> !existing.getId().equals(id)).ifPresent(existing -> {
            throw new DataIntegrityViolationException("Пользователь уже существует.");
        });
        boolean securityChanged = !user.getUsername().equals(username)
                || user.getUserRole() != request.role() || user.isEnabled() != request.enabled();
        user.setUsername(username);
        user.setFullName(request.fullName());
        user.setUserRole(request.role());
        user.setEnabled(request.enabled());
        users.save(user);
        if (securityChanged || !user.isEnabled()) sessions.revokeAll(id);
        return response(user);
    }

    @NotNull
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public UserResponse setEnabled(@NotNull UUID userId, boolean enabled) {
        User user = users.findByIdForUpdate(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден."));
        boolean changed = user.isEnabled() != enabled;
        user.setEnabled(enabled);
        users.save(user);
        if (changed || !enabled) sessions.revokeAll(userId);
        return response(user);
    }

    @NotNull
    @PreAuthorize("isAuthenticated() and #username == authentication.name")
    @Transactional(readOnly = true)
    public UserResponse currentUser(@NotNull String username) {
        return users.findByUsername(username).map(this::response)
                .orElseThrow(() -> new BadCredentialsException("Учётная запись недоступна."));
    }

    @PreAuthorize("isAuthenticated() and #username == authentication.name")
    @Transactional
    public void changePassword(@NotNull String username, @NotNull ChangePasswordRequest request) {
        var user = users.findByUsernameForUpdate(username)
                .orElseThrow(() -> new BadCredentialsException("Учётная запись недоступна."));
        if (!user.isEnabled()) throw new DisabledException("Учётная запись отключена.");
        UserCredentials account = credentials.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("Учётная запись недоступна."));
        if (!encoder.matches(request.currentPassword(), account.getPasswordHash())) {
            throw new BadCredentialsException("Неверное имя пользователя или пароль.");
        }
        if (encoder.matches(request.newPassword(), account.getPasswordHash())) {
            throw new IllegalArgumentException("Новый пароль должен отличаться от текущего.");
        }
        account.setPasswordHash(encoder.encode(request.newPassword()));
        account.setMustChangePassword(false);
        credentials.save(account);
        sessions.revokeAll(user.getId());
        audit.record(AuditEventType.PASSWORD_CHANGED, AuditEntityType.USER, user.getId(), "Пароль изменён пользователем.");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void resetPassword(@NotNull UUID userId, @NotNull String password) {
        User user = users.findByIdForUpdate(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден."));
        UserCredentials account = credentials.findById(userId)
                .orElseGet(() -> new UserCredentials(user, ""));
        account.setPasswordHash(encoder.encode(password));
        account.setMustChangePassword(true);
        credentials.save(account);
        sessions.revokeAll(userId);
        audit.record(AuditEventType.PASSWORD_RESET, AuditEntityType.USER, userId, "Администратор задал временный пароль.");
    }

    @NotNull
    private UserResponse response(@NotNull User user) {
        boolean mustChangePassword = credentials.findById(user.getId())
                .map(UserCredentials::isMustChangePassword).orElse(false);
        return UserResponse.of(user, mustChangePassword);
    }

    @NotNull
    private User findUser(@NotNull UUID id) {
        return users.findById(id).orElseThrow(() -> new UserNotFoundException("Пользователь не найден."));
    }
}
