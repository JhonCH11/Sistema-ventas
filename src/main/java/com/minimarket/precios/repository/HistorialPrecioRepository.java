package com.minimarket.precios.repository;

import com.minimarket.precios.domain.HistorialPrecio;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistorialPrecioRepository extends JpaRepository<HistorialPrecio, Long> {
}
