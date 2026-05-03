package com.example.nexus.modules.inventory.service;

import com.example.nexus.exception.ValidationException;
import com.example.nexus.modules.inventory.entity.ProductTypeConfig;
import com.example.nexus.modules.inventory.repository.ProductTypeConfigRepository;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reglas de inventario por tipo de producto (tabla {@code product_type_config}).
 * Caché en memoria por tipo: se invalida solo al reiniciar la aplicación; al agregar tipos en BD,
 * reiniciar o llamar a {@link #clearCache()} si se expone un endpoint administrativo.
 */
@Service
public class ProductTypeConfigService {

    private final ProductTypeConfigRepository repository;
    private final ConcurrentHashMap<String, ProductTypeConfig> cache = new ConcurrentHashMap<>();

    public ProductTypeConfigService(ProductTypeConfigRepository repository) {
        this.repository = repository;
    }

    /**
     * Normaliza el código de tipo para coincidir con filas en BD (trim + mayúsculas).
     */
    public String normalizeProductType(String productType) {
        if (productType == null) {
            return null;
        }
        return productType.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * Configuración por tipo; resultado cacheado en memoria por tipo normalizado.
     */
    public ProductTypeConfig getConfigByProductType(String productType) {
        String key = normalizeProductType(productType);
        if (key == null || key.isEmpty()) {
            throw new ValidationException("El tipo de producto es obligatorio");
        }
        return cache.computeIfAbsent(key, this::loadRequired);
    }

    /**
     * Comprueba que exista la fila de configuración sin exigir producto completo.
     */
    public void assertProductTypeConfigured(String productType) {
        getConfigByProductType(productType);
    }

    public void clearCache() {
        cache.clear();
    }

    private ProductTypeConfig loadRequired(String normalizedKey) {
        return repository.findById(normalizedKey)
                .orElseThrow(() -> new ValidationException(
                        "El tipo de producto no está configurado: " + normalizedKey));
    }
}
