package com.app.civify.security;

import com.app.civify.entity.User;
import com.app.civify.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;
import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w!#$%&'*+/=?`{|}~^-]+(?:\\.[\\w!#$%&'*+/=?`{|}~^-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}$");

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {

        Optional<User> userOptional;

        // 1. Detect the input type using regex
        if (isEmail(usernameOrEmail)) {
            // 2. If it looks like an email, fetch only by email
            userOptional = userRepository.findByEmail(usernameOrEmail);

            if (userOptional.isEmpty()) {
                throw new UsernameNotFoundException(
                        "User not found with email: " + usernameOrEmail);
            }
        } else {
            // 3. If it does NOT look like an email, fetch only by username
            userOptional = userRepository.findByUsername(usernameOrEmail);

            if (userOptional.isEmpty()) {
                throw new UsernameNotFoundException(
                        "User not found with username: " + usernameOrEmail);
            }
        }

        // 4. Return the UserDetails (UserPrincipal)
        return new UserPrincipal(userOptional.get());
    }

    /**
     * Helper method to check if the input string matches the email regex
     * pattern.
     */
    private boolean isEmail(String input) {
        if (input == null) {
            return false;
        }
        return EMAIL_PATTERN.matcher(input).matches();
    }
}
