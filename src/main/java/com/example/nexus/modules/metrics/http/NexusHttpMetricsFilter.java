package com.example.nexus.modules.metrics.http;

import java.io.IOException;

import org.springframework.http.HttpMethod;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.nexus.modules.metrics.service.NexusMetricsService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Instrumenta métricas HTTP (auth y mutaciones de estructura de bodega) a partir de respuestas.
 * <p>
 * Debe ejecutarse <strong>antes</strong> que Spring Security (ver {@code MetricsHttpFilterConfiguration})
 * para que los 403 y demás estados se observen en {@code finally} tras {@code chain.doFilter}.
 * <p>
 * Auth: prefijo {@code /api/auth} — Timer solo en {@code POST /api/auth/login}.
 * Bodegas: operaciones mutadoras sobre {@code /api/warehouses}, {@code /api/sectors},
 * {@code /api/storage-spaces}.
 */
@RequiredArgsConstructor
public class NexusHttpMetricsFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/auth/login";

    private final NexusMetricsService nexusMetricsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();
        if (path == null || shouldSkip(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        long start = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long elapsed = System.nanoTime() - start;
            int status = response.getStatus();

            if (path.startsWith("/api/auth")) {
                nexusMetricsService.recordAuthHttp(status);
                if (HttpMethod.POST.matches(request.getMethod()) && LOGIN_PATH.equals(path)) {
                    nexusMetricsService.recordAuthLoginDurationNanos(elapsed);
                }
            }

            if (isWarehouseStructureMutation(request.getMethod(), path)) {
                nexusMetricsService.recordWarehouseOperationDurationNanos(elapsed);
                nexusMetricsService.recordWarehouseHttp(status);
            }
        }
    }

    private static boolean shouldSkip(String path) {
        return path.startsWith("/actuator")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/error");
    }

    private static boolean isWarehouseStructureMutation(String method, String path) {
        if (method == null) {
            return false;
        }
        String m = method.toUpperCase();
        if (!("POST".equals(m) || "PUT".equals(m) || "PATCH".equals(m) || "DELETE".equals(m))) {
            return false;
        }
        return path.startsWith("/api/warehouses")
                || path.startsWith("/api/sectors")
                || path.startsWith("/api/storage-spaces");
    }
}
