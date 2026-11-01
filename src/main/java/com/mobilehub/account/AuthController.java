package com.mobilehub.account;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Locale;
import com.mobilehub.config.JwtService;
import com.mobilehub.config.AdminEmailPolicy;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AccountRepository accounts;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AdminEmailPolicy adminEmailPolicy;

    public AuthController(AccountRepository accounts, PasswordEncoder passwordEncoder,
                          AuthenticationManager authenticationManager, JwtService jwtService,
                          AdminEmailPolicy adminEmailPolicy) {
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.adminEmailPolicy = adminEmailPolicy;
    }

    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken token) {
        return new CsrfResponse(token.getToken());
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (request.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            return ResponseEntity.badRequest().body(new ApiMessage("Password must be no more than 72 UTF-8 bytes."));
        }
        if (accounts.existsByEmailIgnoreCase(email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiMessage("An account already exists for this email."));
        }
        Account account = new Account(request.firstName().trim(), request.lastName().trim(), email,
                request.phone().trim(), passwordEncoder.encode(request.password()), request.dateOfBirth(),
                request.state(), request.address() == null ? "" : request.address().trim());
        accounts.save(account);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiMessage("Account created. You can now sign in."));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email().trim(), request.password()));
            Account account = (Account) authentication.getPrincipal();
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, jwtService.createCookie(jwtService.createToken(account)).toString())
                    .body(userResponse(account));
        } catch (BadCredentialsException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiMessage("Email or password is incorrect."));
        }
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        return userResponse((Account) authentication.getPrincipal());
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiMessage> logout() {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtService.clearCookie().toString())
                .body(new ApiMessage("Signed out."));
    }

    private UserResponse userResponse(Account account) {
        return new UserResponse(account.getId(), account.getFirstName(), account.getLastName(),
                account.getEmail(), adminEmailPolicy.isAdmin(account.getEmail()));
    }

    public record RegisterRequest(
            @NotBlank @Size(min = 2, max = 80) String firstName,
            @NotBlank @Size(min = 2, max = 80) String lastName,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Pattern(regexp = "[6-9][0-9]{9}") String phone,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotNull @Past LocalDate dateOfBirth,
            @Size(max = 80) String state,
            @Size(max = 1000) String address) { }

    public record LoginRequest(@NotBlank @Email @Size(max = 254) String email,
                               @NotBlank @Size(max = 72) String password) { }
    public record UserResponse(String id, String firstName, String lastName, String email, boolean admin) { }
    public record ApiMessage(String message) { }
    public record CsrfResponse(String token) { }
}
