package com.phraseloop.service;

import com.phraseloop.domain.User;
import com.phraseloop.dto.UserResponse;
import com.phraseloop.error.ApiException;
import com.phraseloop.repository.UserRepository;
import com.phraseloop.security.AppUserDetails;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse register(String email, String password) {
        String normalizedEmail = normalizeEmail(email);
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "入力内容を確認してください",
                    Map.of("password", "パスワードは72バイト以内で入力してください"));
        }
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw emailTaken();
        }
        User user = new User(normalizedEmail, passwordEncoder.encode(password));
        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw emailTaken();
        }
        return new UserResponse(user.getId(), user.getEmail());
    }

    @Transactional(readOnly = true)
    public AppUserDetails loadRegistered(String email) {
        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "ログインしてください"));
        return new AppUserDetails(user.getId(), user.getEmail(), user.getPasswordHash());
    }

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static ApiException emailTaken() {
        return new ApiException(
                HttpStatus.CONFLICT,
                "EMAIL_ALREADY_EXISTS",
                "このメールアドレスは登録済みです",
                Map.of("email", "このメールアドレスは登録済みです"));
    }
}
