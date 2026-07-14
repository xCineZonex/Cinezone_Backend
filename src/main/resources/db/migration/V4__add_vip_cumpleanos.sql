ALTER TABLE sedes ADD COLUMN vip_cumpleanos_habilitado BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE beneficios_pendientes ADD COLUMN tipo_entrada VARCHAR(50);

CREATE UNIQUE INDEX uk_user_beneficio_year 
ON beneficios_pendientes (usuario_id, tipo_beneficio, EXTRACT(YEAR FROM fecha_ganado));
