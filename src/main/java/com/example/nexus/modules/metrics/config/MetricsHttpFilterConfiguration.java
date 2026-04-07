package com.example.nexus.modules.metrics.config;

import com.example.nexus.modules.metrics.http.NexusHttpMetricsFilter;
import com.example.nexus.modules.metrics.service.NexusMetricsService;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Registra el filtro de métricas HTTP <strong>antes</strong> del {@code FilterChainProxy}
 * de Spring Security (orden por defecto -100). Si el filtro corre después, los 403
 * generados por seguridad no pasan por nuestro {@code finally} y las métricas quedan en 0.
 */
@Configuration
public class MetricsHttpFilterConfiguration {

    /**
     * Orden menor que Spring Security (-100) para envolver toda la cadena incluida seguridad.
     */
    private static final int METRICS_FILTER_ORDER = -200;

    @Bean
    public NexusHttpMetricsFilter nexusHttpMetricsFilter(NexusMetricsService nexusMetricsService) {
        return new NexusHttpMetricsFilter(nexusMetricsService);
    }

    @Bean
    public FilterRegistrationBean<NexusHttpMetricsFilter> nexusHttpMetricsFilterRegistration(
            NexusHttpMetricsFilter nexusHttpMetricsFilter
    ) {
        FilterRegistrationBean<NexusHttpMetricsFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(nexusHttpMetricsFilter);
        registration.addUrlPatterns("/*");
        registration.setOrder(METRICS_FILTER_ORDER);
        return registration;
    }
}
