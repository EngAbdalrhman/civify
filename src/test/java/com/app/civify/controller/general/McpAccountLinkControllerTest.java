package com.app.civify.controller.general;

import com.app.civify.security.UserPrincipal;
import com.app.civify.service.McpAccountLinkService;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class McpAccountLinkControllerTest {
    @Test void refusesCookieOnlyOrMissingUserForConsent() {
        var service = mock(McpAccountLinkService.class);
        var controller = new McpAccountLinkController(service);
        var approval = new McpAccountLinkController.Approval("t".repeat(43));
        assertThrows(ResponseStatusException.class, () -> controller.authorize(approval, null, "Bearer fixture"));
        assertThrows(ResponseStatusException.class, () -> controller.authorize(approval, mock(UserPrincipal.class), null));
        verifyNoInteractions(service);
    }
}
