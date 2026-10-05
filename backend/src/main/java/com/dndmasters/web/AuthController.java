package com.dndmasters.web;

import com.dndmasters.persistence.entity.UserEntity;
import com.dndmasters.persistence.repository.UserRepository;
import com.dndmasters.security.JwtService;
import com.dndmasters.security.SecurityUtils;
import com.dndmasters.web.dto.AuthDtos.AuthResponse;
import com.dndmasters.web.dto.AuthDtos.LoginRequest;
import com.dndmasters.web.dto.AuthDtos.RegisterRequest;
import com.dndmasters.web.dto.AuthDtos.UserView;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final AuthenticationManager authenticationManager;

    public AuthController(UserRepository users, PasswordEncoder encoder, JwtService jwt,
                          AuthenticationManager authenticationManager) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        if (users.existsByUsername(request.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El nombre de usuario ya existe");
        }
        UserEntity user = new UserEntity();
        user.setUsername(request.username().trim());
        user.setPasswordHash(encoder.encode(request.password()));
        user.setDisplayName(request.displayName() == null || request.displayName().isBlank()
                ? request.username() : request.displayName().trim());
        user.setAvatar("/images/avatars/default.png");
        // The very first registered user becomes the administrator.
        user.setRole(users.count() == 0 ? "ADMIN" : "USER");
        user.setCreatedAt(Instant.now().toString());
        users.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseFor(user));
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        UserEntity user = users.findByUsername(request.username())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas"));
        return responseFor(user);
    }

    @GetMapping("/me")
    public UserView me() {
        Long userId = SecurityUtils.requireUserId();
        return users.findById(userId).map(this::view)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado"));
    }

    private AuthResponse responseFor(UserEntity user) {
        return new AuthResponse(jwt.generate(user.getId(), user.getUsername()), view(user));
    }

    private UserView view(UserEntity user) {
        return new UserView(user.getId(), user.getUsername(), user.getDisplayName(), user.getAvatar(), user.getRole());
    }
}
