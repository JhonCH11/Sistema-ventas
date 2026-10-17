package com.minimarket.usuarios.repository;

import com.minimarket.usuarios.domain.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    java.util.List<Usuario> findByRolOrderByNombreAsc(com.minimarket.usuarios.domain.RolUsuario rol);

    Optional<Usuario> findByNombreUsuarioAndActivoTrue(String nombreUsuario);

    boolean existsByNombreUsuarioIgnoreCase(String nombreUsuario);

    boolean existsByNombreUsuarioIgnoreCaseAndIdNot(String nombreUsuario, Long id);
}
