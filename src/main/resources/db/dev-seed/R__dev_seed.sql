-- Datos de DEMO solo para el perfil "dev". Repetible e idempotente.
-- Fechas pensadas para que un tratamiento "de hoy" sea válido.

-- Centros -------------------------------------------------------------
INSERT INTO rescue_centers (code, name, city) VALUES
    ('CTR-001', 'Centro de Rescate Santa Marta', 'Santa Marta'),
    ('CTR-002', 'Centro de Rescate Cartagena',   'Cartagena')
ON CONFLICT (code) DO NOTHING;

-- Casos ---------------------------------------------------------------
INSERT INTO rescue_cases (case_code, rescue_date, rescue_location, status, rescue_center_id)
SELECT v.case_code, v.rescue_date::date, v.rescue_location, v.status, c.id
FROM (VALUES
    ('RES-2026-001', '2026-08-10', 'Playa de Taganga',     'IN_REHABILITATION', 'CTR-001'),
    ('RES-2026-002', '2026-08-18', 'Bahía de Gaira',       'UNDER_EVALUATION',  'CTR-001'),
    ('RES-2026-003', '2026-09-02', 'Isla Barú',            'ADMITTED',          'CTR-002'),
    ('RES-2026-004', '2026-07-21', 'Playa Blanca',         'READY_FOR_RELEASE', 'CTR-002'),
    ('RES-2026-005', '2026-06-15', 'Parque Tayrona',       'RELEASED',          'CTR-001'),
    ('RES-2026-006', '2026-09-20', 'Islas del Rosario',    'IN_REHABILITATION', 'CTR-002'),
    ('RES-2026-007', '2026-10-05', 'Playa Los Cocos',      'ADMITTED',          'CTR-001')
) AS v(case_code, rescue_date, rescue_location, status, center_code)
JOIN rescue_centers c ON c.code = v.center_code
ON CONFLICT (case_code) DO NOTHING;

-- Animales (1:1 con el caso) -----------------------------------------
INSERT INTO animals (animal_code, common_name, scientific_name, sex, rescue_case_id)
SELECT v.animal_code, v.common_name, v.scientific_name, v.sex, rc.id
FROM (VALUES
    ('AN-2026-001', 'Tortuga verde',     'Chelonia mydas',          'MALE',    'RES-2026-001'),
    ('AN-2026-002', 'Delfín nariz de botella', 'Tursiops truncatus','FEMALE',  'RES-2026-002'),
    ('AN-2026-003', 'Pelícano pardo',    'Pelecanus occidentalis',  'UNKNOWN', 'RES-2026-003'),
    ('AN-2026-004', 'Tortuga carey',     'Eretmochelys imbricata',  'FEMALE',  'RES-2026-004'),
    ('AN-2026-005', 'Manatí del Caribe', 'Trichechus manatus',      'MALE',    'RES-2026-005'),
    ('AN-2026-006', 'Tortuga caguama',   'Caretta caretta',         'FEMALE',  'RES-2026-006'),
    ('AN-2026-007', 'Lobo marino',       'Otaria flavescens',       'MALE',    'RES-2026-007')
) AS v(animal_code, common_name, scientific_name, sex, case_code)
JOIN rescue_cases rc ON rc.case_code = v.case_code
ON CONFLICT (animal_code) DO NOTHING;

-- Expedientes médicos --------------------------------------------------
INSERT INTO medical_records (animal_id, initial_weight, initial_condition, injuries, observations)
SELECT a.id, v.weight::numeric, v.condition, v.injuries, v.observations
FROM (VALUES
    ('AN-2026-001', '42.50', 'Debilitado',  'Herida en aleta delantera izquierda', 'Enredada en red de pesca'),
    ('AN-2026-002', '180.00','Estable',     'Laceraciones superficiales',          'Varamiento en orilla'),
    ('AN-2026-006', '61.20', 'Deshidratada','Ingestión de plástico (sospecha)',    'En observación')
) AS v(animal_code, weight, condition, injuries, observations)
JOIN animals a ON a.animal_code = v.animal_code
ON CONFLICT (animal_id) DO NOTHING;

-- Especialistas --------------------------------------------------------
INSERT INTO specialists (professional_code, first_name, last_name, email, active) VALUES
    ('SPEC-001', 'Elena',  'Vargas',  'elena.vargas@deepblue.com',  TRUE),
    ('SPEC-002', 'Carlos', 'Mendoza', 'carlos.mendoza@deepblue.com', TRUE),
    ('SPEC-003', 'Lucía',  'Pardo',   'lucia.pardo@deepblue.com',   FALSE)
ON CONFLICT (professional_code) DO NOTHING;

INSERT INTO specialist_expertise (specialist_id, expertise_id)
SELECT s.id, e.id
FROM (VALUES
    ('SPEC-001', 'Marine Reptiles'),
    ('SPEC-001', 'Rehabilitation'),
    ('SPEC-002', 'Marine Mammals'),
    ('SPEC-002', 'Trauma'),
    ('SPEC-003', 'Toxicology')
) AS v(professional_code, expertise_name)
JOIN specialists s ON s.professional_code = v.professional_code
JOIN expertise  e ON e.name = v.expertise_name
ON CONFLICT DO NOTHING;

-- Tratamientos ---------------------------------------------------------
INSERT INTO treatments (animal_id, specialist_id, performed_at, type, description)
SELECT a.id, s.id, v.performed_at::timestamp, v.type, v.description
FROM (VALUES
    ('AN-2026-001', 'SPEC-001', '2026-08-11 09:00:00', 'WOUND_CARE',    'Limpieza y curación de herida en aleta delantera.'),
    ('AN-2026-001', 'SPEC-001', '2026-08-14 10:30:00', 'MEDICATION',    'Antibiótico de amplio espectro durante 5 días.'),
    ('AN-2026-002', 'SPEC-002', '2026-08-19 08:15:00', 'HYDRATION',     'Reposición de líquidos y monitoreo de signos vitales.'),
    ('AN-2026-006', 'SPEC-001', '2026-09-21 11:00:00', 'OBSERVATION',   'Observación por posible ingestión de plástico.')
) AS v(animal_code, specialist_code, performed_at, type, description)
JOIN animals a     ON a.animal_code = v.animal_code
JOIN specialists s ON s.professional_code = v.specialist_code
WHERE NOT EXISTS (
    SELECT 1 FROM treatments t
    WHERE t.animal_id = a.id AND t.specialist_id = s.id
      AND t.performed_at = v.performed_at::timestamp
);
