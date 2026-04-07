package com.app.civify.service.auth;

import com.app.civify.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class ExtensionOAuthService {

    @Value("${app.chrome-extension.id:}")
    private String chromeExtensionId;

    public void validateProvider(String provider) {
        if (!"google".equals(provider) && !"linkedin".equals(provider)) {
            throw new BadRequestException("extension.oauth.invalid.provider");
        }
    }

    public void validateExtensionRedirectUri(String redirectUri) {
        if (chromeExtensionId == null || chromeExtensionId.isBlank()) {
            throw new BadRequestException("extension.oauth.not.enabled");
        }
        if (redirectUri == null || redirectUri.isBlank()) {
            throw new BadRequestException("extension.oauth.invalid.redirect");
        }

        String decoded = redirectUri;
        try {
            decoded = java.net.URLDecoder.decode(redirectUri, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            // If it's not URL-encoded, keep original value.
        }

        try {
            URI uri = URI.create(decoded);
            // Chrome Identity redirect URI format:
            // - chrome-extension://<EXTENSION_ID>/<path>
            // - https://<EXTENSION_ID>.chromiumapp.org/<path>
            if ("chrome-extension".equals(uri.getScheme())) {
                if (!chromeExtensionId.equals(uri.getHost())) {
                    throw new BadRequestException("extension.oauth.invalid.redirect");
                }
                return;
            }
            if ("https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null) {
                String expectedHost = chromeExtensionId + ".chromiumapp.org";
                if (!expectedHost.equalsIgnoreCase(uri.getHost())) {
                    throw new BadRequestException("extension.oauth.invalid.redirect");
                }
                return;
            }
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception ignored) {
            // fall through to generic invalid
        }

        throw new BadRequestException("extension.oauth.invalid.redirect");
    }

    public String buildSpringAuthorizationRedirectUrl(
            String provider,
            String redirectUri,
            String contextPath
    ) {
        validateProvider(provider);
        validateExtensionRedirectUri(redirectUri);

        String safeContextPath = contextPath == null ? "" : contextPath;
        if (!safeContextPath.startsWith("/")) {
            safeContextPath = "/" + safeContextPath;
        }

        return safeContextPath + "/oauth2/authorization/" + provider
                + "?redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);
    }
}

