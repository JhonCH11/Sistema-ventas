package com.minimarket.ventas.repository;

import com.minimarket.ventas.domain.MedioPago;
import com.minimarket.ventas.domain.EstadoVenta;
import com.minimarket.ventas.domain.Pago;
import java.math.BigDecimal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PagoRepository extends JpaRepository<Pago, Long> {
    java.util.Optional<Pago> findByVentaId(Long ventaId);

    @Query("""
            select sum(pago.importePagado)
            from Pago pago
            where pago.venta.aperturaCaja.id = :aperturaCajaId
              and pago.venta.estado = :estadoVenta
              and pago.medioPago = :medioPago
            """)
    BigDecimal totalCobradoPorAperturaYMedioPago(
            @Param("aperturaCajaId") Long aperturaCajaId,
            @Param("medioPago") MedioPago medioPago,
            @Param("estadoVenta") EstadoVenta estadoVenta);
}
