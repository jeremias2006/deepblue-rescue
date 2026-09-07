package com.deepblue.rescue;

import java.util.List;
import com.deepblue.rescue.domain.*;
import com.deepblue.rescue.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;


import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@Transactional
class PersistenceIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:18-alpine")
                    .withDatabaseName("deepblue_test")
                    .withUsername("deepblue")
                    .withPassword("deepblue");

    @Autowired
    private RescueCenterRepository rescueCenterRepository;

    @Autowired
    private RescueCaseRepository rescueCaseRepository;

    @Autowired
    private AnimalRepository animalRepository;

    @Autowired
    private MedicalRecordRepository medicalRecordRepository;

    @Autowired
    private SpecialistRepository specialistRepository;

    @Autowired
    private ExpertiseRepository expertiseRepository;

    @Autowired
    private TreatmentRepository treatmentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void contextLoads() {
        assertThat(postgres.isRunning()).isTrue();
    }

    @Test
    void flywayEjecutoLasMigraciones() {
        var migraciones = jdbcTemplate.queryForList(
                "SELECT version, description FROM flyway_schema_history ORDER BY installed_rank"
        );

        assertThat(migraciones).hasSize(3);
        assertThat(migraciones.get(0).get("version")).isEqualTo("1");
        assertThat(migraciones.get(1).get("version")).isEqualTo("2");
        assertThat(migraciones.get(2).get("version")).isEqualTo("3");
    }

    @Test
    void metodosHeredadosFuncionanCorrectamente() {
        RescueCenter centro = new RescueCenter("DB-CAR", "DeepBlue Caribbean", "Santa Marta");

        RescueCenter guardado = rescueCenterRepository.save(centro);

        assertThat(guardado.getId()).isNotNull();
        assertThat(rescueCenterRepository.existsById(guardado.getId())).isTrue();
        assertThat(rescueCenterRepository.findById(guardado.getId())).isPresent();
        assertThat(rescueCenterRepository.count()).isEqualTo(1);
    }

    @Test
    void unCentroTieneMuchosCasos() {
        RescueCenter centro = rescueCenterRepository.save(
                new RescueCenter("DB-CAR", "DeepBlue Caribbean", "Santa Marta")
        );

        RescueCase caso1 = new RescueCase("RES-2026-001", java.time.LocalDate.of(2026, 6, 10),
                "Playa Blanca", RescueStatus.ADMITTED);
        RescueCase caso2 = new RescueCase("RES-2026-002", java.time.LocalDate.of(2026, 6, 15),
                "Bahía Concha", RescueStatus.UNDER_EVALUATION);

        centro.addCase(caso1);
        centro.addCase(caso2);

        rescueCaseRepository.save(caso1);
        rescueCaseRepository.save(caso2);

        List<RescueCase> casosDelCentro = rescueCaseRepository.findByRescueCenterCode("DB-CAR");

        assertThat(casosDelCentro).hasSize(2);
        assertThat(casosDelCentro)
                .extracting(RescueCase::getCaseCode)
                .containsExactlyInAnyOrder("RES-2026-001", "RES-2026-002");
    }

    @Test
    void unCasoTieneUnAnimal() {
        RescueCenter centro = rescueCenterRepository.save(
                new RescueCenter("DB-CAR", "DeepBlue Caribbean", "Santa Marta")
        );

        RescueCase caso = new RescueCase("RES-2026-001", java.time.LocalDate.of(2026, 6, 10),
                "Playa Blanca", RescueStatus.ADMITTED);
        centro.addCase(caso);
        rescueCaseRepository.save(caso);

        Animal animal = new Animal("AN-2026-001", "Tortuga Verde", "Chelonia mydas", AnimalSex.FEMALE);
        caso.assignAnimal(animal);
        animalRepository.save(animal);

        RescueCase casoRecuperado = rescueCaseRepository.findByCaseCode("RES-2026-001").orElseThrow();
        Animal animalRecuperado = animalRepository.findByAnimalCode("AN-2026-001").orElseThrow();

        assertThat(casoRecuperado.getAnimal().getAnimalCode()).isEqualTo("AN-2026-001");
        assertThat(animalRecuperado.getRescueCase().getCaseCode()).isEqualTo("RES-2026-001");
    }

    @Test
    void unAnimalTieneUnExpedienteMedico() {
        RescueCenter centro = rescueCenterRepository.save(
                new RescueCenter("DB-CAR", "DeepBlue Caribbean", "Santa Marta")
        );
        RescueCase caso = new RescueCase("RES-2026-010", java.time.LocalDate.of(2026, 6, 10),
                "Playa Blanca", RescueStatus.ADMITTED);
        centro.addCase(caso);
        rescueCaseRepository.save(caso);

        Animal animal = new Animal("AN-2026-010", "Tortuga Verde", "Chelonia mydas", AnimalSex.FEMALE);
        caso.assignAnimal(animal);
        animalRepository.save(animal);

        MedicalRecord expediente = new MedicalRecord(
                new java.math.BigDecimal("45.50"), "Deshidratada", "Herida en aleta", "Requiere observación"
        );
        animal.assignMedicalRecord(expediente);
        animalRepository.save(animal);

        Animal animalRecuperado = animalRepository.findByAnimalCode("AN-2026-010").orElseThrow();

        assertThat(animalRecuperado.getMedicalRecord()).isNotNull();
        assertThat(animalRecuperado.getMedicalRecord().getId()).isNotNull();
        assertThat(animalRecuperado.getMedicalRecord().getInitialWeight())
                .isEqualByComparingTo("45.50");
    }

    @Test
    void unEspecialistaTieneVariasExpertise() {
        Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma").orElseThrow();
        Expertise rehabilitacion = expertiseRepository.findByNameIgnoreCase("Rehabilitation").orElseThrow();

        Specialist elena = new Specialist("SP-001", "Elena", "Vargas", "elena.vargas@deepblue.org");
        elena.addExpertise(trauma);
        elena.addExpertise(rehabilitacion);
        specialistRepository.save(elena);

        Specialist elenaRecuperada = specialistRepository.findById(elena.getId()).orElseThrow();

        assertThat(elenaRecuperada.getExpertiseAreas()).hasSize(2);
        assertThat(elenaRecuperada.getExpertiseAreas())
                .extracting(Expertise::getName)
                .containsExactlyInAnyOrder("Trauma", "Rehabilitation");
    }

    @Test
    void filtrarCasosPorStatus() {
        RescueCenter centro = rescueCenterRepository.save(
                new RescueCenter("DB-CAR", "DeepBlue Caribbean", "Santa Marta")
        );

        RescueCase caso1 = new RescueCase("RES-2026-020", java.time.LocalDate.of(2026, 5, 1),
                "Zona A", RescueStatus.IN_REHABILITATION);
        RescueCase caso2 = new RescueCase("RES-2026-021", java.time.LocalDate.of(2026, 5, 2),
                "Zona B", RescueStatus.IN_REHABILITATION);
        RescueCase caso3 = new RescueCase("RES-2026-022", java.time.LocalDate.of(2026, 5, 3),
                "Zona C", RescueStatus.CLOSED);

        centro.addCase(caso1);
        centro.addCase(caso2);
        centro.addCase(caso3);
        rescueCaseRepository.save(caso1);
        rescueCaseRepository.save(caso2);
        rescueCaseRepository.save(caso3);

        List<RescueCase> enRehabilitacion =
                rescueCaseRepository.findByStatusOrderByRescueDateAsc(RescueStatus.IN_REHABILITATION);

        assertThat(enRehabilitacion).hasSize(2);
    }

    @Test
    void animalesDeUnCentroNoSeMezclanConOtro() {
        RescueCenter centroA = rescueCenterRepository.save(
                new RescueCenter("DB-CAR", "DeepBlue Caribbean", "Santa Marta")
        );
        RescueCenter centroB = rescueCenterRepository.save(
                new RescueCenter("DB-PAC", "DeepBlue Pacific", "Buenaventura")
        );

        RescueCase casoA = new RescueCase("RES-2026-030", java.time.LocalDate.of(2026, 5, 1),
                "Zona A", RescueStatus.ADMITTED);
        centroA.addCase(casoA);
        rescueCaseRepository.save(casoA);
        Animal animalA = new Animal("AN-2026-030", "Tortuga Carey", "Eretmochelys imbricata", AnimalSex.MALE);
        casoA.assignAnimal(animalA);
        animalRepository.save(animalA);

        RescueCase casoB = new RescueCase("RES-2026-031", java.time.LocalDate.of(2026, 5, 2),
                "Zona B", RescueStatus.ADMITTED);
        centroB.addCase(casoB);
        rescueCaseRepository.save(casoB);
        Animal animalB = new Animal("AN-2026-031", "Delfín Nariz de Botella", "Tursiops truncatus", AnimalSex.FEMALE);
        casoB.assignAnimal(animalB);
        animalRepository.save(animalB);

        List<Animal> animalesDeA = animalRepository.findByRescueCaseRescueCenterCode("DB-CAR");

        assertThat(animalesDeA).hasSize(1);
        assertThat(animalesDeA.get(0).getAnimalCode()).isEqualTo("AN-2026-030");
    }

    @Test
    void buscarEspecialistasActivosPorExpertise() {
        Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma").orElseThrow();
        Expertise toxicologia = expertiseRepository.findByNameIgnoreCase("Toxicology").orElseThrow();

        Specialist elena = new Specialist("SP-010", "Elena", "Vargas", "elena@deepblue.org");
        elena.addExpertise(trauma);
        specialistRepository.save(elena);

        Specialist mateo = new Specialist("SP-011", "Mateo", "Rios", "mateo@deepblue.org");
        mateo.addExpertise(toxicologia);
        specialistRepository.save(mateo);

        Specialist sofia = new Specialist("SP-012", "Sofia", "Lopez", "sofia@deepblue.org");
        sofia.addExpertise(trauma);
        specialistRepository.save(sofia);

        List<Specialist> conTrauma = specialistRepository.findActiveByExpertise("trauma");

        assertThat(conTrauma).hasSize(2);
        assertThat(conTrauma)
                .extracting(Specialist::getLastName)
                .containsExactlyInAnyOrder("Vargas", "Lopez");
    }

    @Test
    void tratamientosDeUnAnimalEnOrdenCronologico() {
        RescueCenter centro = rescueCenterRepository.save(
                new RescueCenter("DB-CAR", "DeepBlue Caribbean", "Santa Marta")
        );
        RescueCase caso = new RescueCase("RES-2026-040", java.time.LocalDate.of(2026, 6, 1),
                "Zona A", RescueStatus.IN_REHABILITATION);
        centro.addCase(caso);
        rescueCaseRepository.save(caso);

        Animal animal = new Animal("AN-2026-040", "Tortuga Verde", "Chelonia mydas", AnimalSex.FEMALE);
        caso.assignAnimal(animal);
        animalRepository.save(animal);

        Specialist especialista = new Specialist("SP-020", "Elena", "Vargas", "elena.v@deepblue.org");
        specialistRepository.save(especialista);

        Treatment t1 = new Treatment(animal, especialista,
                java.time.LocalDateTime.of(2026, 8, 5, 9, 0), TreatmentType.WOUND_CARE, "Limpieza de herida");
        Treatment t2 = new Treatment(animal, especialista,
                java.time.LocalDateTime.of(2026, 8, 1, 8, 0), TreatmentType.HYDRATION, "Suero");
        Treatment t3 = new Treatment(animal, especialista,
                java.time.LocalDateTime.of(2026, 8, 10, 10, 0), TreatmentType.OBSERVATION, "Chequeo general");

        treatmentRepository.save(t1);
        treatmentRepository.save(t2);
        treatmentRepository.save(t3);

        List<Treatment> tratamientos =
                treatmentRepository.findByAnimalIdOrderByPerformedAtAsc(animal.getId());

        assertThat(tratamientos).hasSize(3);
        assertThat(tratamientos)
                .extracting(Treatment::getType)
                .containsExactly(TreatmentType.HYDRATION, TreatmentType.WOUND_CARE, TreatmentType.OBSERVATION);
    }

    @Test
    void tratamientosEntreFechas() {
        RescueCenter centro = rescueCenterRepository.save(
                new RescueCenter("DB-CAR", "DeepBlue Caribbean", "Santa Marta")
        );
        RescueCase caso = new RescueCase("RES-2026-050", java.time.LocalDate.of(2026, 6, 1),
                "Zona A", RescueStatus.IN_REHABILITATION);
        centro.addCase(caso);
        rescueCaseRepository.save(caso);

        Animal animal = new Animal("AN-2026-050", "Tortuga Verde", "Chelonia mydas", AnimalSex.FEMALE);
        caso.assignAnimal(animal);
        animalRepository.save(animal);

        Specialist especialista = new Specialist("SP-030", "Mateo", "Rios", "mateo.r@deepblue.org");
        specialistRepository.save(especialista);

        treatmentRepository.save(new Treatment(animal, especialista,
                java.time.LocalDateTime.of(2026, 8, 5, 9, 0), TreatmentType.WOUND_CARE, "Día 5"));
        treatmentRepository.save(new Treatment(animal, especialista,
                java.time.LocalDateTime.of(2026, 8, 15, 9, 0), TreatmentType.HYDRATION, "Día 15"));
        treatmentRepository.save(new Treatment(animal, especialista,
                java.time.LocalDateTime.of(2026, 8, 25, 9, 0), TreatmentType.OBSERVATION, "Día 25"));

        List<Treatment> enRangoMedio = treatmentRepository.findBetweenDates(
                java.time.LocalDateTime.of(2026, 8, 10, 0, 0),
                java.time.LocalDateTime.of(2026, 8, 20, 0, 0)
        );

        assertThat(enRangoMedio).hasSize(1);
        assertThat(enRangoMedio.get(0).getDescription()).isEqualTo("Día 15");
    }

    @Test
    void noSePuedeDuplicarElCodigoDeUnAnimal() {
        RescueCenter centro = rescueCenterRepository.save(
                new RescueCenter("DB-CAR", "DeepBlue Caribbean", "Santa Marta")
        );
        RescueCase caso1 = new RescueCase("RES-2026-060", java.time.LocalDate.of(2026, 6, 1),
                "Zona A", RescueStatus.ADMITTED);
        centro.addCase(caso1);
        rescueCaseRepository.save(caso1);

        Animal animal1 = new Animal("AN-100", "Tortuga Verde", "Chelonia mydas", AnimalSex.FEMALE);
        caso1.assignAnimal(animal1);
        animalRepository.saveAndFlush(animal1);

        RescueCase caso2 = new RescueCase("RES-2026-061", java.time.LocalDate.of(2026, 6, 2),
                "Zona B", RescueStatus.ADMITTED);
        centro.addCase(caso2);
        rescueCaseRepository.save(caso2);

        Animal animal2 = new Animal("AN-100", "Delfín", "Tursiops truncatus", AnimalSex.MALE);
        caso2.assignAnimal(animal2);

        org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.dao.DataIntegrityViolationException.class,
                () -> animalRepository.saveAndFlush(animal2)
        );
    }

    @Test
    void escenarioCompletoDeUnaTortugaRescatada() {
        // 1. Centro
        RescueCenter centro = rescueCenterRepository.save(
                new RescueCenter("DB-CAR", "DeepBlue Caribbean", "Santa Marta")
        );

        // 2. Caso
        RescueCase caso = new RescueCase("RES-2026-100", java.time.LocalDate.of(2026, 7, 1),
                "Playa Blanca", RescueStatus.IN_REHABILITATION);
        centro.addCase(caso);
        rescueCaseRepository.save(caso);

        // 3. Animal
        Animal tortuga = new Animal("AN-2026-100", "Tortuga Verde", "Chelonia mydas", AnimalSex.FEMALE);
        caso.assignAnimal(tortuga);
        animalRepository.save(tortuga);

        // 4. Expediente médico
        MedicalRecord expediente = new MedicalRecord(
                new java.math.BigDecimal("60.00"), "Herida por anzuelo", "Aleta derecha", "En recuperación"
        );
        tortuga.assignMedicalRecord(expediente);
        animalRepository.save(tortuga);

        // 5. Especialista con varias expertise
        Expertise reptiles = expertiseRepository.findByNameIgnoreCase("Marine Reptiles").orElseThrow();
        Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma").orElseThrow();
        Expertise rehabilitacion = expertiseRepository.findByNameIgnoreCase("Rehabilitation").orElseThrow();

        Specialist elena = new Specialist("SP-100", "Elena", "Vargas", "elena.vargas.100@deepblue.org");
        elena.addExpertise(reptiles);
        elena.addExpertise(trauma);
        elena.addExpertise(rehabilitacion);
        specialistRepository.save(elena);

        // 6. Dos tratamientos
        treatmentRepository.save(new Treatment(tortuga, elena,
                java.time.LocalDateTime.of(2026, 7, 2, 9, 0), TreatmentType.WOUND_CARE, "Limpieza de herida"));
        treatmentRepository.save(new Treatment(tortuga, elena,
                java.time.LocalDateTime.of(2026, 7, 5, 9, 0), TreatmentType.PHYSIOTHERAPY, "Terapia de aleta"));

        // ---- Las 8 consultas del escenario ----

        // a) Existencia del caso (método heredado)
        assertThat(rescueCaseRepository.findByCaseCode("RES-2026-100")).isPresent();

        // b) Casos por status (Query Method)
        assertThat(rescueCaseRepository.findByStatusOrderByRescueDateAsc(RescueStatus.IN_REHABILITATION))
                .extracting(RescueCase::getCaseCode)
                .contains("RES-2026-100");

        // c) Animales por centro (Query Method navegado)
        assertThat(animalRepository.findByRescueCaseRescueCenterCode("DB-CAR"))
                .extracting(Animal::getAnimalCode)
                .contains("AN-2026-100");

        // d) Búsqueda por nombre común (Query Method con Containing)
        assertThat(animalRepository.findByCommonNameContainingIgnoreCase("tortuga"))
                .extracting(Animal::getAnimalCode)
                .contains("AN-2026-100");

        // e) Especialistas por expertise (JPQL)
        assertThat(specialistRepository.findActiveByExpertise("Trauma"))
                .extracting(Specialist::getProfessionalCode)
                .contains("SP-100");

        // f) Tratamientos de un animal (Query Method)
        assertThat(treatmentRepository.findByAnimalIdOrderByPerformedAtAsc(tortuga.getId()))
                .hasSize(2);

        // g) Tratamientos por expertise del especialista (JPQL con N:M)
        assertThat(treatmentRepository.findByExpertise("Trauma"))
                .extracting(Treatment::getDescription)
                .contains("Limpieza de herida", "Terapia de aleta");

        // h) Tratamientos por intervalo de fechas (JPQL con between)
        assertThat(treatmentRepository.findBetweenDates(
                java.time.LocalDateTime.of(2026, 7, 1, 0, 0),
                java.time.LocalDateTime.of(2026, 7, 3, 0, 0)
        )).extracting(Treatment::getDescription)
                .containsExactly("Limpieza de herida");
    }

    @Test
    void animalesEnRehabilitacionTratadosPorEspecialistaConTrauma() {
        RescueCenter centro = rescueCenterRepository.save(
                new RescueCenter("DB-CAR", "DeepBlue Caribbean", "Santa Marta")
        );

        Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma").orElseThrow();
        Expertise toxicologia = expertiseRepository.findByNameIgnoreCase("Toxicology").orElseThrow();

        Specialist elena = new Specialist("SP-200", "Elena", "Vargas", "elena.200@deepblue.org");
        elena.addExpertise(trauma);
        specialistRepository.save(elena);

        Specialist mateo = new Specialist("SP-201", "Mateo", "Rios", "mateo.201@deepblue.org");
        mateo.addExpertise(toxicologia);
        specialistRepository.save(mateo);

        // Animal 1: en rehabilitación, tratado por Elena (Trauma) -> SÍ debe aparecer
        RescueCase caso1 = new RescueCase("RES-2026-200", java.time.LocalDate.of(2026, 6, 1),
                "Zona A", RescueStatus.IN_REHABILITATION);
        centro.addCase(caso1);
        rescueCaseRepository.save(caso1);
        Animal animal1 = new Animal("AN-2026-200", "Tortuga Verde", "Chelonia mydas", AnimalSex.FEMALE);
        caso1.assignAnimal(animal1);
        animalRepository.save(animal1);
        treatmentRepository.save(new Treatment(animal1, elena,
                java.time.LocalDateTime.of(2026, 6, 2, 9, 0), TreatmentType.WOUND_CARE, "Cura"));

        // Animal 2: en rehabilitación, pero tratado por Mateo (Toxicology) -> NO debe aparecer
        RescueCase caso2 = new RescueCase("RES-2026-201", java.time.LocalDate.of(2026, 6, 3),
                "Zona B", RescueStatus.IN_REHABILITATION);
        centro.addCase(caso2);
        rescueCaseRepository.save(caso2);
        Animal animal2 = new Animal("AN-2026-201", "Delfín", "Tursiops truncatus", AnimalSex.MALE);
        caso2.assignAnimal(animal2);
        animalRepository.save(animal2);
        treatmentRepository.save(new Treatment(animal2, mateo,
                java.time.LocalDateTime.of(2026, 6, 4, 9, 0), TreatmentType.MEDICATION, "Tratamiento tóxico"));

        // Animal 3: tratado por Elena (Trauma), pero YA CERRADO -> NO debe aparecer
        RescueCase caso3 = new RescueCase("RES-2026-202", java.time.LocalDate.of(2026, 6, 5),
                "Zona C", RescueStatus.CLOSED);
        centro.addCase(caso3);
        rescueCaseRepository.save(caso3);
        Animal animal3 = new Animal("AN-2026-202", "Tortuga Carey", "Eretmochelys imbricata", AnimalSex.MALE);
        caso3.assignAnimal(animal3);
        animalRepository.save(animal3);
        treatmentRepository.save(new Treatment(animal3, elena,
                java.time.LocalDateTime.of(2026, 6, 6, 9, 0), TreatmentType.WOUND_CARE, "Cura final"));

        List<Animal> resultado = animalRepository.findInRehabilitationTreatedByExpertise(
                RescueStatus.IN_REHABILITATION, "trauma"
        );

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getAnimalCode()).isEqualTo("AN-2026-200");
    }
}