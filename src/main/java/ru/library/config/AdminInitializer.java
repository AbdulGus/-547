package ru.library.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.library.entity.*;
import ru.library.repository.UserRepository;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    @Value("${app.admin.email}")
    private String email;
    @Value("${app.admin.password}")
    private String password;

    @Override
    @Transactional
    public void run(String... args) {
        if (email.isBlank() && password.isBlank()) {
            return;
        }
        if (!email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+") || !password.matches("[\\x21-\\x7E]{8,64}")) {
            throw new IllegalStateException("Set a valid ADMIN_EMAIL and an 8-64 character ASCII ADMIN_PASSWORD");
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmail(normalized)) {
            return;
        }
        AppUser admin = new AppUser();
        admin.setEmail(normalized);
        admin.setPassword(encoder.encode(password));
        admin.setRole(Role.ADMIN);
        users.save(admin);
    }
}
