package com.team.corporate.backend.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {
    public static final String HEADER = "X-Correlation-ID";
    public static final String MDC_KEY = "correlationId";
    public static final String ATTRIBUTE = CorrelationIdFilter.class.getName() + ".id";
    public static final String VALID_ID = "[A-Za-z0-9][A-Za-z0-9._-]{0,63}";
    private static final Pattern ID_PATTERN = Pattern.compile(VALID_ID);
    private static final Logger LOGGER = LoggerFactory.getLogger(CorrelationIdFilter.class);

    public static String currentId() {
        String value = MDC.get(MDC_KEY);
        return value != null && ID_PATTERN.matcher(value).matches() ? value : null;
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() { return false; }

    @Override
    protected boolean shouldNotFilterErrorDispatch() { return false; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String id = request.getAttribute(ATTRIBUTE) instanceof String existing
                && ID_PATTERN.matcher(existing).matches() ? existing : requestId(request);
        request.setAttribute(ATTRIBUTE, id);
        response.setHeader(HEADER, id);
        String previous = MDC.get(MDC_KEY);
        MDC.put(MDC_KEY, id);
        boolean completed = false;
        try {
            chain.doFilter(request, response);
            completed = true;
        } finally {
            try {
                String path = request.getRequestURI().replaceAll("[\\r\\n\\t]", "_");
                if (path.length() > 512) path = path.substring(0, 512);
                LOGGER.info("HTTP {} {} completed with status {}", request.getMethod(), path,
                        completed ? response.getStatus() : 500);
            } finally {
                if (previous == null) MDC.remove(MDC_KEY);
                else MDC.put(MDC_KEY, previous);
            }
        }
    }

    private static String requestId(HttpServletRequest request) {
        var values = request.getHeaders(HEADER);
        if (values != null && values.hasMoreElements()) {
            String value = values.nextElement();
            if (!values.hasMoreElements() && ID_PATTERN.matcher(value).matches()) return value;
        }
        return UUID.randomUUID().toString();
    }
}
