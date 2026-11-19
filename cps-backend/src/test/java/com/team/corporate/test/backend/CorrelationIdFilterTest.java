package com.team.corporate.test.backend;

import com.team.corporate.backend.filter.CorrelationIdFilter;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class CorrelationIdFilterTest {
    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @AfterEach
    void cleanUp() { MDC.clear(); }

    @Test
    void validIncomingIdReachesResponseRequestMdcAndLogsWithoutLoggingSecrets() throws Exception {
        var request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.addHeader(CorrelationIdFilter.HEADER, "client-request_123");
        request.setQueryString("password=do-not-log-this");
        request.setContent("secret-request-body".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var response = new MockHttpServletResponse();
        AtomicReference<String> loggedId = new AtomicReference<>();
        AtomicReference<String> loggedMessage = new AtomicReference<>();
        var logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(CorrelationIdFilter.class);
        var appender = new ch.qos.logback.core.AppenderBase<ch.qos.logback.classic.spi.ILoggingEvent>() {
            @Override
            protected void append(ch.qos.logback.classic.spi.ILoggingEvent event) {
                loggedId.set(event.getMDCPropertyMap().get(CorrelationIdFilter.MDC_KEY));
                loggedMessage.set(event.getFormattedMessage());
            }
        };
        appender.start();
        logger.addAppender(appender);
        try {
            filter.doFilter(request, response, (req, res) -> {
                assertEquals("client-request_123", MDC.get(CorrelationIdFilter.MDC_KEY));
                assertEquals("client-request_123", req.getAttribute(CorrelationIdFilter.ATTRIBUTE));
            });
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
        assertEquals("client-request_123", response.getHeader(CorrelationIdFilter.HEADER));
        assertEquals("client-request_123", loggedId.get());
        assertFalse(loggedMessage.get().contains("do-not-log-this"));
        assertFalse(loggedMessage.get().contains("secret-request-body"));
        assertNull(MDC.get(CorrelationIdFilter.MDC_KEY));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "spaces are invalid", "bad\r\nid", "bad,id",
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"})
    void invalidHeaderIsReplacedWithAGeneratedId(String value) throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/auth/me");
        request.addHeader(CorrelationIdFilter.HEADER, value);
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> { });
        assertDoesNotThrow(() -> UUID.fromString(response.getHeader(CorrelationIdFilter.HEADER)));
        assertNull(MDC.get(CorrelationIdFilter.MDC_KEY));
    }

    @Test
    void missingOrDuplicateIdsAreGeneratedIndependently() throws Exception {
        var firstRequest = new MockHttpServletRequest("GET", "/first");
        var secondRequest = new MockHttpServletRequest("GET", "/second");
        secondRequest.addHeader(CorrelationIdFilter.HEADER, "first-value");
        secondRequest.addHeader(CorrelationIdFilter.HEADER, "second-value");
        var first = new MockHttpServletResponse();
        var second = new MockHttpServletResponse();
        filter.doFilter(firstRequest, first, (req, res) -> { });
        filter.doFilter(secondRequest, second, (req, res) -> { });
        UUID one = UUID.fromString(first.getHeader(CorrelationIdFilter.HEADER));
        UUID two = UUID.fromString(second.getHeader(CorrelationIdFilter.HEADER));
        assertNotEquals(one, two);
    }

    @Test
    void previousMdcIsRestoredWhenTheChainThrows() {
        MDC.put(CorrelationIdFilter.MDC_KEY, "outer-context");
        var request = new MockHttpServletRequest("GET", "/failure");
        request.addHeader(CorrelationIdFilter.HEADER, "failed-request");
        var response = new MockHttpServletResponse();
        assertThrows(ServletException.class, () -> filter.doFilter(request, response, (req, res) -> {
            assertEquals("failed-request", MDC.get(CorrelationIdFilter.MDC_KEY));
            throw new ServletException("probe");
        }));
        assertEquals("outer-context", MDC.get(CorrelationIdFilter.MDC_KEY));
        assertEquals("failed-request", response.getHeader(CorrelationIdFilter.HEADER));
    }
}
