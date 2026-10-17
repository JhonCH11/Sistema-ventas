CREATE TABLE extensiones_horario_caja (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    apertura_caja_id BIGINT UNSIGNED NOT NULL,
    usuario_id BIGINT UNSIGNED NOT NULL,
    hora_anterior TIME NOT NULL,
    hora_nueva TIME NOT NULL,
    motivo VARCHAR(200) NOT NULL,
    autorizada_en DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    KEY idx_extensiones_apertura (apertura_caja_id),
    CONSTRAINT fk_extension_apertura FOREIGN KEY (apertura_caja_id) REFERENCES aperturas_caja(id),
    CONSTRAINT fk_extension_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
    CONSTRAINT chk_extension_hora CHECK (hora_nueva > hora_anterior AND hora_nueva <= '23:59:00')
);
