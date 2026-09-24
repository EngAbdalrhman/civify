package com.app.civify.util;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

class RequestLoggingFilterTest {
    @Test
    void correlatesDeniedRequestsWithoutLoggingCredentialsOrPersonalData() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(RequestLoggingFilter.class);
        var appender = new ListAppender<ILoggingEvent>(); appender.start(); logger.addAppender(appender);
        try {
            var request = new MockHttpServletRequest("POST", "/apis/v1/external/cvs/tailor");
            request.addHeader("X-Request-ID", "38d18bbe-a723-4c20-8a70-430527e9f5fa");
            request.addHeader("X-Civify-MCP-Tool", "civify_tailor_cv");
            request.addHeader("X-API-KEY", "secret-key-fixture");
            request.addHeader("User-Agent", "private-agent-fixture");
            request.setQueryString("email=private-email-fixture");
            request.setContent("private-resume-fixture".getBytes());
            var response = new MockHttpServletResponse();
            MDC.put("parent", "keep");
            new RequestLoggingFilter().doFilter(request, response, (req, res) -> response.setStatus(401));
            assertEquals("38d18bbe-a723-4c20-8a70-430527e9f5fa", response.getHeader("X-Request-ID"));
            assertEquals("keep", MDC.get("parent"));
            assertNull(MDC.get("requestId"));
            String logs = appender.list.stream().map(ILoggingEvent::getFormattedMessage).reduce("", String::concat);
            assertTrue(logs.contains("mcpTool=civify_tailor_cv"));
            assertTrue(logs.contains("operation=cv_tailor"));
            assertTrue(logs.contains("status=401"));
            assertTrue(logs.contains("success=false"));
            assertFalse(logs.contains("secret-key-fixture"));
            assertFalse(logs.contains("private-"));
        } finally { logger.detachAppender(appender); MDC.clear(); }
    }

    @Test
    void rejectsForgedLogFieldsAndMarksUncaughtExceptionsAsFailures() {
        var request = new MockHttpServletRequest("POST", "/apis/v1/external/cvs/tailor");
        request.addHeader("X-Request-ID", "bad\nforged-log");
        request.addHeader("X-Civify-MCP-Tool", "bad\nforged-log");
        var response = new MockHttpServletResponse();
        assertThrows(ServletException.class, () -> new RequestLoggingFilter().doFilter(request, response,
                (req, res) -> { assertEquals("none", MDC.get("mcpTool")); throw new ServletException("failure"); }));
        assertDoesNotThrow(() -> java.util.UUID.fromString(response.getHeader("X-Request-ID")));
        assertNull(MDC.get("requestId"));
    }
}
