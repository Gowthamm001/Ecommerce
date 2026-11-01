package com.mobilehub.config;

import com.mobilehub.account.Account;
import com.mobilehub.account.AccountRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final AccountRepository accounts;
    private final AdminEmailPolicy adminEmailPolicy;

    public JwtAuthenticationFilter(JwtService jwtService, AccountRepository accounts,
                                   AdminEmailPolicy adminEmailPolicy) {
        this.jwtService = jwtService;
        this.accounts = accounts;
        this.adminEmailPolicy = adminEmailPolicy;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = getToken(request);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                String accountId = jwtService.getSubject(token);
                accounts.findById(accountId).ifPresent(account -> {
                    var authorities = adminEmailPolicy.isAdmin(account.getEmail())
                            ? List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"),
                                    new SimpleGrantedAuthority("ROLE_ADMIN"))
                            : account.getAuthorities();
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(account, null, authorities));
                });
            } catch (JwtException | IllegalArgumentException exception) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    private String getToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        return Arrays.stream(cookies)
                .filter(cookie -> JwtService.COOKIE_NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
