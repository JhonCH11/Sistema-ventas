package com.minimarket.caja.repository;

import com.minimarket.caja.domain.MovimientoDineroCaja;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface MovimientoDineroCajaRepository extends JpaRepository<MovimientoDineroCaja, Long> {
    @EntityGraph(attributePaths = {"usuario", "reversionDe"})
    List<MovimientoDineroCaja> findByAperturaCajaIdOrderByIdDesc(Long aperturaId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<MovimientoDineroCaja> findByToken(String token);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<MovimientoDineroCaja> findByReversionDeId(Long id);

    @Query("select coalesce(sum(m.impacto),0) from MovimientoDineroCaja m where m.aperturaCaja.id=:id")
    BigDecimal impactoTotal(@Param("id") Long aperturaId);
}
