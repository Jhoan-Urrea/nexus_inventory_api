package com.example.nexus.modules.metrics.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.nexus.modules.auth.entity.AuthAuditEventType;
import com.example.nexus.modules.metrics.NexusMetricsConstants;
import com.example.nexus.modules.metrics.repository.WarehouseMetricsQuery;
import com.example.nexus.modules.warehouse.entity.Warehouse;
import com.example.nexus.modules.warehouse.repository.WarehouseRepository;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MultiGauge;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

/**
 * Registro central de métricas Nexus (Micrometer → Prometheus).
 * Expone contadores y timers crudos; tasas y porcentajes se calculan en Prometheus/Grafana.
 */
@Service
@RequiredArgsConstructor
public class NexusMetricsService {

    private final MeterRegistry meterRegistry;
    private final WarehouseRepository warehouseRepository;
    private final WarehouseMetricsQuery warehouseMetricsQuery;

    private Counter authLoginSuccessTotal;
    private Counter authLoginFailedTotal;
    private Counter authLoginAttemptsTotal;
    private Counter authLogoutTotal;

    private Timer authLoginDuration;

    private Counter warehouseHttpRequestsCounter;
    private Counter warehouseHttp4xxCounter;
    private Counter warehouseHttp5xxCounter;

    private Timer warehouseOperationsDuration;

    private Counter authHttpRequestsCounter;
    private Counter authHttp4xxCounter;
    private Counter authHttp5xxCounter;
    private Counter authHttpSecurityRejectCounter;

    private MultiGauge warehouseOccupancyMultiGauge;
    private MultiGauge warehouseStorageSpacesMultiGauge;
    private MultiGauge warehouseSectorsMultiGauge;

    @PostConstruct
    void registerMeters() {
        authLoginSuccessTotal = Counter.builder(NexusMetricsConstants.AUTH_LOGIN_SUCCESS_TOTAL)
                .description("Logins exitosos (LOGIN_SUCCESS)")
                .register(meterRegistry);

        authLoginFailedTotal = Counter.builder(NexusMetricsConstants.AUTH_LOGIN_FAILED_TOTAL)
                .description("Logins fallidos (LOGIN_FAILED)")
                .register(meterRegistry);

        authLoginAttemptsTotal = Counter.builder(NexusMetricsConstants.AUTH_LOGIN_ATTEMPTS_TOTAL)
                .description("Intentos de login (éxito + fallo)")
                .register(meterRegistry);

        authLogoutTotal = Counter.builder(NexusMetricsConstants.AUTH_LOGOUT_TOTAL)
                .description("Cierres de sesión (LOGOUT)")
                .register(meterRegistry);

        authLoginDuration = Timer.builder(NexusMetricsConstants.AUTH_LOGIN_DURATION)
                .description("Tiempo de respuesta POST /api/auth/login")
                .publishPercentiles(0.95)
                .register(meterRegistry);

        authHttpRequestsCounter = Counter.builder(NexusMetricsConstants.AUTH_HTTP_REQUESTS_TOTAL)
                .description("Solicitudes HTTP a /api/auth/**")
                .register(meterRegistry);

        authHttp4xxCounter = Counter.builder(NexusMetricsConstants.AUTH_HTTP_4XX_TOTAL)
                .description("Respuestas HTTP 4xx en /api/auth/**")
                .register(meterRegistry);

        authHttp5xxCounter = Counter.builder(NexusMetricsConstants.AUTH_HTTP_5XX_TOTAL)
                .description("Respuestas HTTP 5xx en /api/auth/**")
                .register(meterRegistry);

        authHttpSecurityRejectCounter = Counter.builder(NexusMetricsConstants.AUTH_HTTP_SECURITY_REJECT_TOTAL)
                .description("Respuestas HTTP 403 en /api/auth/**")
                .register(meterRegistry);

        warehouseOperationsDuration = Timer.builder(NexusMetricsConstants.WAREHOUSE_OPERATIONS_DURATION)
                .description("Operaciones mutadoras de estructura (bodegas/sectores/espacios)")
                .publishPercentiles(0.95)
                .register(meterRegistry);

        warehouseHttpRequestsCounter = Counter.builder(NexusMetricsConstants.WAREHOUSE_HTTP_REQUESTS_TOTAL)
                .description("Solicitudes en mutaciones de estructura de bodega")
                .register(meterRegistry);

        warehouseHttp4xxCounter = Counter.builder(NexusMetricsConstants.WAREHOUSE_HTTP_4XX_TOTAL)
                .description("Respuestas 4xx en mutaciones de estructura de bodega")
                .register(meterRegistry);

        warehouseHttp5xxCounter = Counter.builder(NexusMetricsConstants.WAREHOUSE_HTTP_5XX_TOTAL)
                .description("Respuestas 5xx en mutaciones de estructura de bodega")
                .register(meterRegistry);

        warehouseOccupancyMultiGauge = MultiGauge.builder(NexusMetricsConstants.WAREHOUSE_OCCUPANCY_RATIO)
                .description("Índice de ocupación por bodega (estado; % capacidad usada)")
                .register(meterRegistry);

        warehouseStorageSpacesMultiGauge = MultiGauge.builder(NexusMetricsConstants.WAREHOUSE_STORAGE_SPACES)
                .description("Espacios de almacenamiento por bodega")
                .register(meterRegistry);

        warehouseSectorsMultiGauge = MultiGauge.builder(NexusMetricsConstants.WAREHOUSE_SECTORS)
                .description("Sectores por bodega")
                .register(meterRegistry);

        refreshWarehouseStructureGauges();
    }

    /**
     * Llamado desde auditoría de auth tras persistir evento.
     */
    public void onAuthAuditEvent(AuthAuditEventType eventType) {
        switch (eventType) {
            case LOGIN_SUCCESS -> {
                authLoginSuccessTotal.increment();
                authLoginAttemptsTotal.increment();
            }
            case LOGIN_FAILED -> {
                authLoginFailedTotal.increment();
                authLoginAttemptsTotal.increment();
            }
            case LOGOUT -> authLogoutTotal.increment();
            default -> {
            }
        }
    }

    public void recordAuthLoginDurationNanos(long nanos) {
        authLoginDuration.record(nanos, TimeUnit.NANOSECONDS);
    }

    public void recordAuthHttp(int status) {
        authHttpRequestsCounter.increment();
        if (status >= 400 && status <= 499) {
            authHttp4xxCounter.increment();
        }
        if (status >= 500 && status <= 599) {
            authHttp5xxCounter.increment();
        }
        if (status == 403) {
            authHttpSecurityRejectCounter.increment();
        }
    }

    public void recordWarehouseOperationDurationNanos(long nanos) {
        warehouseOperationsDuration.record(nanos, TimeUnit.NANOSECONDS);
    }

    public void recordWarehouseHttp(int status) {
        warehouseHttpRequestsCounter.increment();
        if (status >= 400 && status <= 499) {
            warehouseHttp4xxCounter.increment();
        }
        if (status >= 500 && status <= 599) {
            warehouseHttp5xxCounter.increment();
        }
    }

    /**
     * Actualiza MultiGauge de bodegas desde la base de datos.
     */
    @Scheduled(fixedDelayString = "${nexus.metrics.warehouse-gauges-refresh-ms:60000}")
    public void refreshWarehouseStructureGauges() {
        List<Warehouse> warehouses = warehouseRepository.findAllByOrderByNameAsc();

        Map<Long, BigDecimal> usedCapacity = new HashMap<>();
        Map<Long, Long> spaceCount = new HashMap<>();
        for (Object[] row : warehouseMetricsQuery.aggregateStorageCapacityAndCountByWarehouseId()) {
            Long wid = (Long) row[0];
            long count = ((Number) row[1]).longValue();
            BigDecimal sum = toBigDecimal(row[2]);
            usedCapacity.put(wid, sum);
            spaceCount.put(wid, count);
        }

        Map<Long, Long> sectorsByWarehouse = new HashMap<>();
        for (Object[] row : warehouseMetricsQuery.countSectorsByWarehouseId()) {
            sectorsByWarehouse.put((Long) row[0], ((Number) row[1]).longValue());
        }

        List<MultiGauge.Row<?>> occRows = new ArrayList<>();
        List<MultiGauge.Row<?>> spaceRows = new ArrayList<>();
        List<MultiGauge.Row<?>> sectorRows = new ArrayList<>();

        for (Warehouse w : warehouses) {
            Tags tags = Tags.of("warehouse_id", String.valueOf(w.getId()));
            long sectors = sectorsByWarehouse.getOrDefault(w.getId(), 0L);
            long spaces = spaceCount.getOrDefault(w.getId(), 0L);
            BigDecimal used = usedCapacity.getOrDefault(w.getId(), BigDecimal.ZERO);

            double occupancy = 0.0;
            if (w.getTotalCapacityM2() != null && w.getTotalCapacityM2().compareTo(BigDecimal.ZERO) > 0) {
                occupancy = used.multiply(BigDecimal.valueOf(100))
                        .divide(w.getTotalCapacityM2(), 4, RoundingMode.HALF_UP)
                        .doubleValue();
            }

            occRows.add(MultiGauge.Row.of(tags, occupancy));
            spaceRows.add(MultiGauge.Row.of(tags, (double) spaces));
            sectorRows.add(MultiGauge.Row.of(tags, (double) sectors));
        }

        warehouseOccupancyMultiGauge.register(occRows, true);
        warehouseStorageSpacesMultiGauge.register(spaceRows, true);
        warehouseSectorsMultiGauge.register(sectorRows, true);
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        if (value instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        return new BigDecimal(value.toString());
    }
}
