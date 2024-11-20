package com.technical.task.task_project.tracing;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@AllArgsConstructor
public class TraceIdFilter extends OncePerRequestFilter {

    private final Tracer tracer;
    private final Propagator propagator;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Span currentSpan = tracer.currentSpan();
        try (Tracer.SpanInScope spanInScope = tracer.withSpan(currentSpan)) {
            String traceId = currentSpan != null
                    ? currentSpan.context().traceId()
                    : "No Trace Context Available";

            response.setHeader("X-Trace-Id", traceId);

            filterChain.doFilter(request, response);
        }
    }
}
