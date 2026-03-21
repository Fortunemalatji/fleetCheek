package Fleet.check.controller;

import Fleet.check.dto.LoginRequest;
import Fleet.check.dto.LoginResponse;
import Fleet.check.entity.User;
import Fleet.check.repository.UserRepository;
import Fleet.check.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Login with userId + pin. Returns a JWT token valid for 24 hours.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            Authentication auth = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUserId(), request.getPin())
            );

            // Find user by username or userId (consistent with UserDetailsServiceImpl)
            User user = userRepository.findByUsername(request.getUserId())
                    .or(() -> userRepository.findById(request.getUserId()))
                    .orElseThrow();
            
            String role = user.getRole() != null ? user.getRole().getName() : "DRIVER";
            String token = jwtUtil.generateToken(user.getUserId(), role);

            return ResponseEntity.ok(new LoginResponse(token, user.getUserId(), user.getFullName(), role));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid userId or PIN."));
        }
    }

    /**
     * Set or reset a user's PIN (must be called after the user is created via /api/users).
     * Body: { "userId": "0000827008", "pin": "1234" }
     */
    @PostMapping("/set-pin")
    public ResponseEntity<?> setPin(@RequestBody LoginRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "User not found: " + request.getUserId()));
        }
        user.setPinHash(passwordEncoder.encode(request.getPin()));
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "PIN set successfully for " + user.getFullName()));
    }
}
