-- La aplicación accede solo por JDBC con el rol dueño de las tablas. En Supabase existen además los
-- roles anon y authenticated, usados por su API de datos: se les quita todo acceso a este schema.
-- En un PostgreSQL sin esos roles (por ejemplo, el de las pruebas) el bloque no hace nada.
DO $$
DECLARE
    rol TEXT;
BEGIN
    FOREACH rol IN ARRAY ARRAY['anon', 'authenticated'] LOOP
        IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = rol) THEN
            EXECUTE format('REVOKE ALL ON SCHEMA %I FROM %I', '${flyway:defaultSchema}', rol);
            EXECUTE format('REVOKE ALL ON ALL TABLES IN SCHEMA %I FROM %I', '${flyway:defaultSchema}', rol);
            EXECUTE format('REVOKE ALL ON ALL SEQUENCES IN SCHEMA %I FROM %I', '${flyway:defaultSchema}', rol);
        END IF;
    END LOOP;
END
$$;

-- Defensa adicional: RLS activado sin políticas bloquea a cualquier rol que no sea el dueño de la tabla.
-- El dueño (el usuario con el que conecta la aplicación y que ejecutó las migraciones) no se ve afectado.
ALTER TABLE tipos_mascota ENABLE ROW LEVEL SECURITY;
ALTER TABLE mascotas ENABLE ROW LEVEL SECURITY;
ALTER TABLE usuarios ENABLE ROW LEVEL SECURITY;
ALTER TABLE adopciones ENABLE ROW LEVEL SECURITY;
