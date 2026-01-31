package com.memory.context.engine.api.controller;

import com.memory.context.engine.domain.user.entity.User;
import com.memory.context.engine.domain.user.repository.UserRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", "Username already exists"));
        }

        // Password complexity validation
        String password = request.getPassword();
        if (password == null || password.length() < 8 ||
                !password.matches(".*\\d.*") ||
                !password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message",
                    "Password must be at least 8 characters long, contains a number and a special character"));
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(password))
                .role("ROLE_USER")
                .enabled(true)
                .build();

        userRepository.save(user);
        log.info("New user registered: {}", user.getUsername());
        return ResponseEntity.ok(java.util.Map.of("message", "User registered successfully"));
    }

    @DeleteMapping("/me")
    public ResponseEntity<?> deleteAccount(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body(java.util.Map.of("message", "Not authenticated"));
        }
        userRepository.delete(user);
        log.info("User deleted account: {}", user.getUsername());
        return ResponseEntity.ok(java.util.Map.of("message", "Account deleted successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body(java.util.Map.of("message", "Not authenticated"));
        }
        return ResponseEntity.ok(user);
    }

    @Data
    public static class SignupRequest {
        private String username;
        private String password;
    }
}
