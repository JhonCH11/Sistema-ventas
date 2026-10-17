ALTER TABLE cierres_caja
    ADD COLUMN vigente BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN reabierta_por BIGINT UNSIGNED NULL,
    ADD COLUMN motivo_reapertura VARCHAR(200) NULL,
    ADD COLUMN reabierta_en DATETIME(3) NULL,
    ADD COLUMN apertura_vigente BIGINT UNSIGNED GENERATED ALWAYS AS (CASE WHEN vigente = TRUE THEN apertura_caja_id ELSE NULL END) STORED,
    ADD KEY idx_cierres_apertura (apertura_caja_id),
    ADD UNIQUE KEY uk_cierre_vigente (apertura_vigente),
    ADD CONSTRAINT fk_cierre_reabierta_usuario FOREIGN KEY (reabierta_por) REFERENCES usuarios(id);

ALTER TABLE cierres_caja DROP INDEX uk_cierres_caja_apertura;
