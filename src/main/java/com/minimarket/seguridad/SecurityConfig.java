package com.minimarket.seguridad;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, com.minimarket.usuarios.repository.UsuarioRepository usuarios) throws Exception {
        http
                .authorizeHttpRequests(autorizacion -> autorizacion
                        .requestMatchers("/css/**", "/configuracion/inicial", "/ingresar").permitAll()
                        .requestMatchers("/precios/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/colaboradores", "/colaboradores/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/stock/ajustes/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/stock/liquidaciones", "/stock/liquidaciones/**", "/stock/mermas", "/stock/mermas/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/reportes/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/importaciones/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/compras", "/compras/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/caja/horario/extender").hasRole("ADMINISTRADOR")
                        .requestMatchers("/caja/reapertura", "/caja/reapertura/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/caja/movimientos", "/caja/movimientos/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/caja/historial", "/caja/historial/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/configuracion/negocio", "/configuracion/negocio/**").hasRole("ADMINISTRADOR")
                        .anyRequest().authenticated())
                .formLogin(formulario -> formulario
                        .loginPage("/ingresar")
                        .loginProcessingUrl("/ingresar")
                        .defaultSuccessUrl("/", true)
                        .permitAll())
                .logout(cierre -> cierre
                        .logoutSuccessUrl("/ingresar?salio")
                        .permitAll());
        http.addFilterBefore(new UsuarioActivoFilter(usuarios), org.springframework.security.web.access.intercept.AuthorizationFilter.class);
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
