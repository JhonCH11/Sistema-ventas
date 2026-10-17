package com.minimarket.caja.repository;

import com.minimarket.caja.domain.AperturaCaja;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AperturaCajaRepository extends JpaRepository<AperturaCaja, Long> {

    Optional<AperturaCaja> findByFechaOperacion(LocalDate fechaOperacion);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select a from AperturaCaja a where a.fechaOperacion = :fecha")
    Optional<AperturaCaja> bloquearPorFecha(@org.springframework.data.repository.query.Param("fecha") LocalDate fecha);
}
