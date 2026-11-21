package com.venky.assistant.security;

import com.venky.assistant.user.AppUser;
import com.venky.assistant.user.AppUserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(
            AppUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public record RegisterRequest(
            @Email @NotBlank String email,
            @NotBlank @Size(min = 8, max = 128) String password,
            String timezone
    ) {}

    public record LoginRequest(
            @Email @NotBlank String email,
            @NotBlank String password
    ) {}

    public record AuthResponse(String token, String email, UUID userId) {}

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            return ResponseEntity.badRequest().build();
        }

        AppUser user = new AppUser(
                UUID.randomUUID(),
                req.email(),
                passwordEncoder.encode(req.password()),
                req.timezone() != null ? req.timezone() : "Asia/Kolkata",
                Instant.now()
        );
        userRepository.save(user);

        String token = jwtService.generateToken(user.getId(), user.getEmail());
        return ResponseEntity.ok(new AuthResponse(token, user.getEmail(), user.getId()));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return userRepository.findByEmail(req.email())
                .filter(u -> passwordEncoder.matches(req.password(), u.getPasswordHash()))
                .map(u -> {
                    String token = jwtService.generateToken(u.getId(), u.getEmail());
                    return ResponseEntity.ok(new AuthResponse(token, u.getEmail(), u.getId()));
                })
                .orElse(ResponseEntity.status(401).build());
    }
}
