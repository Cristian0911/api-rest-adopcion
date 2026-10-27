-- Datos iniciales. ON CONFLICT evita duplicados si los tipos ya existían.
INSERT INTO tipos_mascota (nombre)
VALUES ('Perro'), ('Gato'), ('Conejo'), ('Ave')
ON CONFLICT (nombre) DO NOTHING;
