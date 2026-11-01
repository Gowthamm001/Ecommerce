package com.mobilehub.config;

import com.mobilehub.account.Account;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    public static final String COOKIE_NAME = "ACCESS_TOKEN";

    private final SecretKey signingKey;
    private final long expirationSeconds;
    private final boolean secureCookie;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-seconds:3600}") long expirationSeconds,
                      @Value("${app.jwt.cookie-secure:false}") boolean secureCookie) {
        if (expirationSeconds <= 0) {
            throw new IllegalArgumentException("JWT expiration must be greater than zero.");
        }
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.expirationSeconds = expirationSeconds;
        this.secureCookie = secureCookie;
    }

    public String createToken(Account account) {
        Instant issuedAt = Instant.now();
        return Jwts.builder()
                .subject(account.getId())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(issuedAt.plusSeconds(expirationSeconds)))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public String getSubject(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }

    public ResponseCookie createCookie(String token) {
        return ResponseCookie.from(COOKIE_NAME, token)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Lax")
                .path("/")
                .maxAge(expirationSeconds)
                .build();
    }

    public ResponseCookie clearCookie() {
        return ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
    }
}
