package com.minimarket.ventas.repository;

import com.minimarket.ventas.domain.VentaDetalle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VentaDetalleRepository extends JpaRepository<VentaDetalle, Long> {
    java.util.List<VentaDetalle> findByVentaIdOrderByIdAsc(Long ventaId);
}
