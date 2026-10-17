package com.minimarket.inicio;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class InicioController {

    private final com.minimarket.usuarios.service.UsuarioService usuarioService;
    private final com.minimarket.stock.service.AlertaStockService alertas;

    public InicioController(com.minimarket.usuarios.service.UsuarioService usuarioService,
                            com.minimarket.stock.service.AlertaStockService alertas) {
        this.usuarioService = usuarioService;
        this.alertas = alertas;
    }

    private static final Locale LOCALE_PERU = Locale.forLanguageTag("es-PE");
    private static final ZoneId ZONA_HORARIA = ZoneId.of("America/Lima");
    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", LOCALE_PERU);
    @GetMapping("/")
    public String mostrarInicio(Model model, Authentication authentication) {
        model.addAttribute("fechaActual", FORMATO_FECHA.format(LocalDate.now(ZONA_HORARIA)));
        model.addAttribute("nombrePersona", usuarioService.obtenerUsuarioActivo(authentication.getName()).getNombre());
        model.addAttribute("alertasStock", alertas.consultar());
        model.addAttribute("administrador", authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRADOR")));
        return "inicio";
    }
}
