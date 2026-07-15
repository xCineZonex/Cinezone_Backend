ALTER TABLE funciones DROP CONSTRAINT IF EXISTS funciones_formato_proyeccion_check;
ALTER TABLE funciones ADD CONSTRAINT funciones_formato_proyeccion_check CHECK (formato_proyeccion in ('FORMAT_2D','FORMAT_3D','IMAX','FORMAT_4DX','VIP'));
