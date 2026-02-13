package com.app.civify.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        if ("linkedin".equalsIgnoreCase(registrationId)) {
            return processLinkedInUser(userRequest, oAuth2User);
        }

        return oAuth2User;
    }

    private OAuth2User processLinkedInUser(OAuth2UserRequest userRequest, OAuth2User oAuth2User) {
        Map<String, Object> attributes = new HashMap<>(oAuth2User.getAttributes());
        String accessToken = userRequest.getAccessToken().getTokenValue();

        // Fetch email from LinkedIn's separate email endpoint
        String email = fetchLinkedInEmail(accessToken);
        if (email != null) {
            attributes.put("email", email);
        }

        // Normalize name: LinkedIn v2/me uses localizedFirstName/localizedLastName
        if (!attributes.containsKey("name")) {
            String firstName = (String) attributes.get("localizedFirstName");
            String lastName = (String) attributes.get("localizedLastName");
            if (firstName != null && lastName != null) {
                attributes.put("name", firstName + " " + lastName);
            } else if (firstName != null) {
                attributes.put("name", firstName);
            }
        }

        // Normalize provider ID: LinkedIn v2/me uses "id", map it to "sub" for consistency with OAuth2SuccessHandler
        if (!attributes.containsKey("sub") && attributes.containsKey("id")) {
            attributes.put("sub", attributes.get("id"));
        }

        return new DefaultOAuth2User(
                oAuth2User.getAuthorities(),
                attributes,
                "id"
        );
    }

    @SuppressWarnings("unchecked")
    private String fetchLinkedInEmail(String accessToken) {
        String emailEndpoint = "https://api.linkedin.com/v2/emailAddress?q=members&projection=(elements*(handle~))";

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    emailEndpoint,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            Map<String, Object> body = response.getBody();
            if (body != null && body.containsKey("elements")) {
                List<Map<String, Object>> elements = (List<Map<String, Object>>) body.get("elements");
                if (!elements.isEmpty()) {
                    Map<String, Object> element = elements.get(0);
                    if (element.containsKey("handle~")) {
                        Map<String, Object> handle = (Map<String, Object>) element.get("handle~");
                        return (String) handle.get("emailAddress");
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch LinkedIn email", e);
        }
        return null;
    }
}
