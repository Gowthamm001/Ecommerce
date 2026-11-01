package com.mobilehub.account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Entity
@Table(name = "accounts")
public class Account implements UserDetails {
    @Id
    private String id;
    private String firstName;
    private String lastName;
    @Column(nullable = false, unique = true, length = 254)
    private String email;
    private String phone;
    @Column(nullable = false)
    private String passwordHash;
    private LocalDate dateOfBirth;
    private String state;
    @Column(length = 1000)
    private String address;

    protected Account() {
    }

    public Account(String firstName, String lastName, String email, String phone, String passwordHash,
                   LocalDate dateOfBirth, String state, String address) {
        this.id = UUID.randomUUID().toString();
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email.toLowerCase(java.util.Locale.ROOT);
        this.phone = phone;
        this.passwordHash = passwordHash;
        this.dateOfBirth = dateOfBirth;
        this.state = state;
        this.address = address;
    }

    public String getId() { return id; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }

    @Override
    public String getUsername() { return email; }

    @Override
    public String getPassword() { return passwordHash; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
    }
}
