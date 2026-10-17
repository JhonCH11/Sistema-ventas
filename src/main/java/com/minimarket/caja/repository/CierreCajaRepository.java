package com.minimarket.caja.repository;

import com.minimarket.caja.domain.CierreCaja;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CierreCajaRepository extends JpaRepository<CierreCaja, Long> {

    boolean existsByAperturaCajaIdAndVigenteTrue(Long aperturaCajaId);

    Optional<CierreCaja> findByAperturaCajaIdAndVigenteTrue(Long aperturaCajaId);
}
