package com.minimarket.respaldos.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

@Component
public class EjecutorRespaldo {
    public void ejecutar(List<String> comando, Path salida) throws IOException, InterruptedException {
        Process proceso = new ProcessBuilder(comando)
                .redirectOutput(salida.toFile())
                .redirectError(ProcessBuilder.Redirect.DISCARD).start();
        try {
            if (!proceso.waitFor(120, TimeUnit.SECONDS)) {
                throw new IOException("El respaldo excedio el tiempo permitido.");
            }
            if (proceso.exitValue() != 0) {
                throw new IOException("MySQL no pudo generar el respaldo.");
            }
        } finally {
            if (proceso.isAlive()) {
                proceso.destroyForcibly();
                proceso.waitFor();
            }
        }
    }
}
