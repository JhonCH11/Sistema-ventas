package com.minimarket.ventas.repository;

import com.minimarket.ventas.domain.Venta;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VentaRepository extends JpaRepository<Venta, Long> {
    java.util.Optional<Venta> findByNumeroTicket(String numeroTicket);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select v from Venta v where v.numeroTicket=:numero")
    java.util.Optional<Venta> bloquearPorTicket(@org.springframework.data.repository.query.Param("numero") String numero);

    List<Venta> findByVendidaEnBetweenOrderByVendidaEnDesc(LocalDateTime inicio, LocalDateTime fin);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "vendedor")
    List<Venta> findByVendidaEnGreaterThanEqualAndVendidaEnLessThanOrderByVendidaEnDesc(
            LocalDateTime inicio, LocalDateTime fin);
}
