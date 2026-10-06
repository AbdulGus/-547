package ru.library.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.library.dto.*;
import ru.library.entity.AppUser;
import ru.library.exception.ApiException;
import ru.library.repository.UserRepository;
import ru.library.security.JwtService;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalize(request.email());
        if (users.existsByEmail(email)) {
            throw ApiException.conflict("Этот email уже зарегистрирован");
        }
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPassword(encoder.encode(request.password()));
        users.saveAndFlush(user);
        return response(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        AppUser user = users.findByEmail(normalize(request.email()))
            .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        if (!encoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }
        return response(user);
    }

    @Transactional(readOnly = true)
    public UserResponse me(String email) {
        AppUser user = users.findByEmail(email).orElseThrow(() -> new BadCredentialsException("Unknown user"));
        return new UserResponse(user.getEmail(), user.getRole().name());
    }

    private AuthResponse response(AppUser user) {
        return new AuthResponse(jwt.issue(user.getEmail()), "Bearer", jwt.expirationSeconds(),
            user.getEmail(), user.getRole().name());
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
