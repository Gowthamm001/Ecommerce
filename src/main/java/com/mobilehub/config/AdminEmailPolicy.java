package com.mobilehub.config;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AdminEmailPolicy {
    private final Set<String> adminEmails;

    public AdminEmailPolicy(@Value("${app.admin.emails:}") String configuredEmails) {
        this.adminEmails = Arrays.stream(configuredEmails.split(","))
                .map(String::trim)
                .filter(email -> !email.isEmpty())
                .map(email -> email.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean isAdmin(String email) {
        return email != null && adminEmails.contains(email.toLowerCase(Locale.ROOT));
    }
}
