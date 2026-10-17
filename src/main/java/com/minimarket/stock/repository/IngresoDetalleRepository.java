package com.minimarket.stock.repository;

import com.minimarket.stock.domain.IngresoDetalle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngresoDetalleRepository extends JpaRepository<IngresoDetalle, Long> {
}
