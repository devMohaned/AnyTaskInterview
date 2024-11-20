package com.technical.task.task_project.security.filter;

import com.technical.task.task_project.security.model.RateLimiter;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Component
@WebFilter(value = {"/authenticate", "/api/users/register"})
@Log4j2
public class RateLimiterWebFilter implements Filter {

    @Value("${task.rate-limit.max-request-count}")
    private int MAX_REQUEST_LIMIT;

    @Value("${task.rate-limit.request-duration}")
    private int REQUEST_DURATION;

    private final ConcurrentHashMap<String, RateLimiter> rateLimiters = new ConcurrentHashMap<>();

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest httpRequest && response instanceof HttpServletResponse httpResponse) {

            // rate limiting based on IP
            String clientIp = httpRequest.getRemoteAddr();
            RateLimiter rateLimiter = rateLimiters.computeIfAbsent(clientIp, key -> new RateLimiter(MAX_REQUEST_LIMIT, Duration.ofMinutes(REQUEST_DURATION)));

            if (!rateLimiter.tryConsume()) {
                httpResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                httpResponse.getWriter().write("Too many requests - try again later");
                log.warn("Client with IP Address [{}] has been trying to access API [{}] for too many requests.", clientIp, httpRequest.getRequestURI());
                return;
            }
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }


}
