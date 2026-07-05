-- V3: Agrega la columna fase_comercial a ticket_base_prices_v2
-- Esta columna es necesaria para las Tarifas Dinámicas por Fase Comercial (Preventa, Estreno, Cartelera)

ALTER TABLE ticket_base_prices_v2
    ADD COLUMN IF NOT EXISTS fase_comercial VARCHAR(20) NOT NULL DEFAULT 'Cartelera';

-- Actualiza el unique constraint para incluir la nueva columna
-- Primero elimina el constraint viejo si existe
ALTER TABLE ticket_base_prices_v2
    DROP CONSTRAINT IF EXISTS uk_ticket_base_prices_v2_ticket_type_formato_beneficio_id;

ALTER TABLE ticket_base_prices_v2
    DROP CONSTRAINT IF EXISTS uktbpv2_type_formato_beneficio;

-- Crea el nuevo unique constraint que incluye fase_comercial
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'ticket_base_prices_v2'
          AND constraint_type = 'UNIQUE'
          AND constraint_name = 'uq_tbpv2_type_formato_beneficio_fase'
    ) THEN
        ALTER TABLE ticket_base_prices_v2
            ADD CONSTRAINT uq_tbpv2_type_formato_beneficio_fase
            UNIQUE (ticket_type, formato, beneficio_id, fase_comercial);
    END IF;
END$$;
