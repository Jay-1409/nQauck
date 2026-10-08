package org.example.statistics;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class PublisherRequestMetricsFilter extends OncePerRequestFilter {

    private static final String EMAIL_SEND_PATH = "/api/email/send";
    private final PublisherRequestMetrics metrics;

    public PublisherRequestMetricsFilter(PublisherRequestMetrics metrics) {
        this.metrics = metrics;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (!"POST".equals(request.getMethod()) || !EMAIL_SEND_PATH.equals(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        long started = System.nanoTime();
        int status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        try {
            filterChain.doFilter(request, response);
            status = response.getStatus();
        } finally {
            metrics.record(status, System.nanoTime() - started);
        }
    }
}
