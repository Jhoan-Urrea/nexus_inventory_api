package com.example.nexus.modules.state.repository;

import com.example.nexus.modules.state.entity.StatusCatalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StatusCatalogRepository extends JpaRepository<StatusCatalog, Long> {
    // Crucial: Permite obtener, por ejemplo, todos los estados de un "Sector"
    List<StatusCatalog> findByEntityTypeId(Long entityTypeId);

    // Buscar un estado específico por su código dentro de una entidad
    Optional<StatusCatalog> findByCodeAndEntityTypeId(String code, Long entityTypeId);
}
