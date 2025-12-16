package com.app.civify.security;

import com.app.civify.entity.User;
import com.app.civify.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User u = super.loadUser(userRequest);
        String provider = userRequest.getClientRegistration().getRegistrationId();
        String subject = u.getName();
        String email = (String) u.getAttributes().getOrDefault("email", subject + "@" + provider + ".oauth");
        User existing = userRepository.findByEmail(email).orElse(null);
        if (existing == null) {
            User user = new User();
            user.setUsername(email);
            user.setEmail(email);
            user.setPassword("{noop}oauth");
            user.setOauthProvider(provider);
            user.setOauthSubject(subject);
            userRepository.save(user);
        }
        return u;
    }
}
