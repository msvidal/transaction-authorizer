package com.github.msvidal.transactionauthorizer.infra.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
public class LoggingFilter implements Filter {

    private static final String REQUEST_ID = "requestId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String requestId = UUID.randomUUID().toString();

        MDC.put(REQUEST_ID, requestId);

        try {
            log.info("Incoming request: {} {}", httpRequest.getMethod(), httpRequest.getRequestURI());
            chain.doFilter(request, response);
            log.info("Request completed: {} {}", httpRequest.getMethod(), httpRequest.getRequestURI());
        } finally {
            MDC.remove(REQUEST_ID);
        }
    }
}

