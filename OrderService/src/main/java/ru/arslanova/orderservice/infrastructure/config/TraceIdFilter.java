package ru.arslanova.orderservice.infrastructure.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {
    public static final String TRACE_HEADER = "X-Trace-Id";
    public static final String MDC_KEY = "traceId";

    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        String trace = request.getHeader(TRACE_HEADER);
        if (trace == null || trace.isBlank()){
            trace = UUID.randomUUID().toString();
        }
        MDC.put(MDC_KEY, trace);
        response.setHeader(TRACE_HEADER, trace);
        try {
            chain.doFilter(request, response);
        }finally {
            MDC.remove(MDC_KEY);
        }
    }
}
