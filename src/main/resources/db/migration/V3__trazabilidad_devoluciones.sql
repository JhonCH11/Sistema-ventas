ALTER TABLE devoluciones
 ADD COLUMN apertura_caja_id BIGINT UNSIGNED NULL,
 ADD COLUMN token_operacion VARCHAR(36) NULL,
 ADD UNIQUE KEY uk_devoluciones_token (token_operacion),
 ADD CONSTRAINT fk_devoluciones_apertura FOREIGN KEY (apertura_caja_id) REFERENCES aperturas_caja (id);
ALTER TABLE devolucion_detalles ADD COLUMN repone_stock BOOLEAN NOT NULL DEFAULT TRUE;

