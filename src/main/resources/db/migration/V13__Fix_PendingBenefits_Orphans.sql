-- Fix too permissive LIKE clause from V12
-- Target only the known corrupted values

UPDATE beneficios_pendientes
SET tipo_beneficio = 'ENTRADA_GRATIS_CUMPLEANOS'
WHERE tipo_beneficio IN (
    'ENTRADA_GRATIS_CUMPLEAOS',
    'ENTRADA_GRATIS_CUMPLEA''OS',
    'ENTRADA_GRATIS_CUMPLEA''OS',
    'ENTRADA_GRATIS_CUMPLEAOS'
);
