package com.team.corporate.backend.security;

import com.team.corporate.backend.dto.ResetPasswordRequest;
import com.team.corporate.backend.audit.AuditEventWriter;
import com.team.corporate.backend.entities.AuditEntityType;
import com.team.corporate.backend.entities.AuditEventType;
import com.team.corporate.backend.entities.UserCredentials;
import com.team.corporate.backend.repositories.SpringUsersRepository;
import com.team.corporate.backend.repositories.UserCredentialsRepository;
import com.team.corporate.core.entities.User;
import com.team.corporate.core.entities.UserRole;
import com.team.corporate.core.utils.EntityValidation;
import jakarta.validation.Validator;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Класс, отвечающий за генерацию первого аккаунта в системе - аккаунта администратора.
 * Имя пользователя и пароль задаются через переменные окружения и конфигурацию приложения.
 */
@Component
public class BootstrapAdmin implements ApplicationRunner {
    private final SpringUsersRepository users;
    private final UserCredentialsRepository credentials;
    private final PasswordEncoder encoder;
    private final Validator validator;
    private final String username;
    private final String password;
    private final AuditEventWriter audit;

    public BootstrapAdmin(
            @NotNull SpringUsersRepository users,
            @NotNull UserCredentialsRepository credentials,
            @NotNull PasswordEncoder encoder,
            @NotNull Validator validator,
            @Value("${cps.security.bootstrap.username:}") String username,
            @Value("${cps.security.bootstrap.password:}") String password,
            @NotNull AuditEventWriter audit
    ) {
        this.users = users;
        this.credentials = credentials;
        this.encoder = encoder;
        this.validator = validator;
        this.username = username;
        this.password = password;
        this.audit = audit;
    }

    @Override
    @Transactional
    public void run(@NotNull ApplicationArguments args) {
        if (username.isBlank() && password.isEmpty()) {
            return;
        }
        if (username.isBlank() || password.isEmpty()) {
            throw new IllegalStateException("Укажите имя и пароль первого администратора.");
        }
        String normalized = EntityValidation.requireText(username, "Имя администратора");
        User user = users.findByUsername(normalized).orElse(null);
        if (user != null && user.getUserRole() != UserRole.ADMIN) {
            throw new IllegalStateException("Учётная запись первого администратора уже существует с другой ролью.");
        }
        if (user != null && credentials.existsById(user.getId())) {
            return;
        }
        if (!validator.validate(new ResetPasswordRequest(password)).isEmpty()) {
            throw new IllegalStateException("Пароль первого администратора: от 12 до 72 символов.");
        }
        if (user == null) {
            user = users.save(new User(normalized, UserRole.ADMIN));
        }
        credentials.save(new UserCredentials(user, encoder.encode(password)));
        audit.recordAs(AuditEventType.BOOTSTRAP_ADMIN_PROVISIONED, AuditEntityType.USER, user.getId(),
                "Задан временный пароль первого администратора.", null, "system");
    }
}
