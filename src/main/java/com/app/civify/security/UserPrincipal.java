package com.app.civify.security;

import com.app.civify.entity.User;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.List;

@RequiredArgsConstructor
public class UserPrincipal implements UserDetails {

    private final User user;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (user == null || user.getRole() == null) {
            return List.of();
        }
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getPassword() {
        return user != null ? user.getPassword() : null;
    }

    @Override
    public String getUsername() {
        return user != null ? user.getUsername() : null;
    }

    @Override
    public boolean isAccountNonExpired() {
        return user != null && user.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return user != null && user.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return user != null && user.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
        return user != null && user.isEnabled();
    }

    public Long getId() {
        return user != null ? user.getId() : null;
    }

    public String getEmail() {
        return user != null ? user.getEmail() : null;
    }

    public String getFirstName() {
        return user != null ? user.getFirstName() : null;
    }

    public String getLastName() {
        return user != null ? user.getLastName() : null;
    }

    public String getPhoneNumber() {
        return user != null ? user.getPhoneNumber() : null;
    }

    public String getFullName() {
        if (user == null) {
            return null;
        }
        String firstName = user.getFirstName();
        String lastName = user.getLastName();
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        }
        return firstName != null ? firstName : lastName;
    }

    public User getUser() {
        return user;
    }
}
