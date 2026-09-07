CREATE TABLE rescue_centers (
                                id BIGSERIAL PRIMARY KEY,
                                code VARCHAR(20) NOT NULL UNIQUE,
                                name VARCHAR(150) NOT NULL,
                                city VARCHAR(100) NOT NULL
);

CREATE TABLE rescue_cases (
                              id BIGSERIAL PRIMARY KEY,
                              case_code VARCHAR(30) NOT NULL UNIQUE,
                              rescue_date DATE NOT NULL,
                              rescue_location VARCHAR(200) NOT NULL,
                              status VARCHAR(30) NOT NULL,
                              rescue_center_id BIGINT NOT NULL REFERENCES rescue_centers(id),
                              CONSTRAINT chk_rescue_case_status CHECK (status IN (
                                                                                  'ADMITTED', 'UNDER_EVALUATION', 'IN_REHABILITATION',
                                                                                  'READY_FOR_RELEASE', 'RELEASED', 'CLOSED'
                                  ))
);

CREATE TABLE animals (
                         id BIGSERIAL PRIMARY KEY,
                         animal_code VARCHAR(30) NOT NULL UNIQUE,
                         common_name VARCHAR(100) NOT NULL,
                         scientific_name VARCHAR(150),
                         sex VARCHAR(10) NOT NULL,
                         rescue_case_id BIGINT NOT NULL UNIQUE REFERENCES rescue_cases(id)
);

CREATE TABLE medical_records (
                                 id BIGSERIAL PRIMARY KEY,
                                 animal_id BIGINT NOT NULL UNIQUE REFERENCES animals(id),
                                 initial_weight NUMERIC(6,2),
                                 initial_condition VARCHAR(200),
                                 injuries VARCHAR(500),
                                 observations VARCHAR(1000)
);

CREATE TABLE specialists (
                             id BIGSERIAL PRIMARY KEY,
                             professional_code VARCHAR(30) NOT NULL UNIQUE,
                             first_name VARCHAR(100) NOT NULL,
                             last_name VARCHAR(100) NOT NULL,
                             email VARCHAR(150) NOT NULL UNIQUE,
                             active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE expertise (
                           id BIGSERIAL PRIMARY KEY,
                           name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE specialist_expertise (
                                      specialist_id BIGINT NOT NULL REFERENCES specialists(id),
                                      expertise_id BIGINT NOT NULL REFERENCES expertise(id),
                                      PRIMARY KEY (specialist_id, expertise_id)
);

CREATE TABLE treatments (
                            id BIGSERIAL PRIMARY KEY,
                            animal_id BIGINT NOT NULL REFERENCES animals(id),
                            specialist_id BIGINT NOT NULL REFERENCES specialists(id),
                            performed_at TIMESTAMP NOT NULL,
                            type VARCHAR(30) NOT NULL,
                            description VARCHAR(1000)
);

CREATE INDEX idx_rescue_case_center ON rescue_cases(rescue_center_id);
CREATE INDEX idx_rescue_case_status ON rescue_cases(status);
CREATE INDEX idx_rescue_case_date ON rescue_cases(rescue_date);
CREATE INDEX idx_treatment_animal ON treatments(animal_id);
CREATE INDEX idx_treatment_specialist ON treatments(specialist_id);
CREATE INDEX idx_treatment_performed_at ON treatments(performed_at);