package com.minimarket.respaldos.service;

import com.minimarket.configuracion.service.ConfiguracionNegocioService;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RespaldoService {
    private final ConfiguracionNegocioService configuracion;
    private final EjecutorRespaldo ejecutor;
    private final String url;
    private final String usuario;
    private final String clave;
    private final String ejecutable;

    public RespaldoService(ConfiguracionNegocioService configuracion, EjecutorRespaldo ejecutor,
                          @Value("${spring.datasource.url}") String url,
                          @Value("${spring.datasource.username}") String usuario,
                          @Value("${spring.datasource.password}") String clave,
                          @Value("${minimarket.respaldos.mysqldump:mysqldump}") String ejecutable) {
        this.configuracion = configuracion;
        this.ejecutor = ejecutor;
        this.url = url;
        this.usuario = usuario;
        this.clave = clave;
        this.ejecutable = ejecutable;
    }

    public synchronized Path generar() {
        Path parcial = null;
        Path credenciales = null;
        try {
            if (!url.startsWith("jdbc:mysql://")) {
                throw new IllegalArgumentException("Se requiere una conexion MySQL local.");
            }
            URI conexion = URI.create(url.replaceFirst("^jdbc:", ""));
            String host = conexion.getHost();
            String base = conexion.getPath() == null || conexion.getPath().isEmpty()
                    ? "" : conexion.getPath().substring(1);
            if (host == null || !List.of("localhost", "127.0.0.1", "[::1]", "::1").contains(host)
                    || !base.matches("[A-Za-z0-9_]+")) {
                throw new IllegalArgumentException("Solo se admiten respaldos de MySQL local.");
            }
            String ruta = configuracion.obtener().getRutaRespaldos();
            Path carpeta = ruta == null || ruta.isBlank()
                    ? Path.of(System.getProperty("user.home"), "Minimarket", "respaldos")
                    : Path.of(ruta);
            if (!carpeta.isAbsolute()) {
                throw new IllegalArgumentException("La carpeta de respaldos debe tener una ruta absoluta.");
            }
            Files.createDirectories(carpeta);
            parcial = Files.createTempFile(carpeta, ".respaldo-", ".parcial");
            credenciales = Files.createTempFile("minimarket-mysql-", ".cnf");
            // La clave no se expone en argumentos de proceso ni en los logs.
            Files.writeString(credenciales, "[client]\nuser=" + opcion(usuario)
                    + "\npassword=" + opcion(clave) + "\n", StandardCharsets.UTF_8);
            ejecutor.ejecutar(List.of(ejecutable, "--defaults-extra-file=" + credenciales,
                    "--host=" + host, "--port=" + (conexion.getPort() < 0 ? 3306 : conexion.getPort()),
                    "--protocol=TCP", "--single-transaction", "--skip-lock-tables",
                    "--no-tablespaces", "--set-gtid-purged=OFF", "--routines", "--events",
                    "--triggers", "--databases", base), parcial);
            if (Files.size(parcial) == 0) {
                throw new IOException("El respaldo esta vacio.");
            }
            String fecha = LocalDateTime.now(ZoneId.of("America/Lima"))
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            Path destino = carpeta.resolve("minimarket_" + fecha + "_" + UUID.randomUUID() + ".sql");
            Files.move(parcial, destino);
            return destino;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Respaldo interrumpido. Reintente desde Configuracion.");
        } catch (IOException | IllegalArgumentException ex) {
            throw new IllegalStateException("No se pudo crear el respaldo. Revise la carpeta, mysqldump y los permisos de MySQL; reintente desde Configuracion.");
        } finally {
            try {
                eliminarTemporal(credenciales);
            } finally {
                eliminarTemporal(parcial);
            }
        }
    }

    private String opcion(String valor) {
        return "\"" + valor.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "\"";
    }

    private void eliminarTemporal(Path ruta) {
        if (ruta != null) {
            try {
                Files.deleteIfExists(ruta);
            } catch (IOException ex) {
                throw new IllegalStateException("No se pudo eliminar un archivo temporal del respaldo. Revise los permisos de las carpetas.");
            }
        }
    }
}
