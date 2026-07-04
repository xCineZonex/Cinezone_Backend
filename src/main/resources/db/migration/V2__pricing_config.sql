ALTER TABLE salas ADD COLUMN IF NOT EXISTS recargo NUMERIC(10,2) DEFAULT 0.00;

CREATE TABLE IF NOT EXISTS system_configuration (
    id BIGINT PRIMARY KEY,
    recargo_estreno NUMERIC(10,2) NOT NULL DEFAULT 0.00,
    dias_estreno INT NOT NULL DEFAULT 7
);

INSERT INTO system_configuration (id, recargo_estreno, dias_estreno) VALUES (1, 5.00, 7) ON CONFLICT (id) DO NOTHING;
