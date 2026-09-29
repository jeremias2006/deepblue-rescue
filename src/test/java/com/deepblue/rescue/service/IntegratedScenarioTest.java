package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.AnimalSex;
import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.domain.Specialist;
import com.deepblue.rescue.domain.Treatment;
import com.deepblue.rescue.domain.TreatmentType;
import com.deepblue.rescue.dto.request.ChangeRescueStatusRequest;
import com.deepblue.rescue.dto.request.CreateTreatmentRequest;
import com.deepblue.rescue.dto.response.TreatmentResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.mapper.RescueCaseMapper;
import com.deepblue.rescue.mapper.TreatmentMapper;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.repository.RescueCaseRepository;
import com.deepblue.rescue.repository.SpecialistRepository;
import com.deepblue.rescue.repository.TreatmentRepository;
import com.deepblue.rescue.service.impl.RescueCaseServiceImpl;
import com.deepblue.rescue.service.impl.TreatmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Reto integrador (Parte XI del laboratorio).
 * Sigue siendo un unit test: los repositories están mockeados,
 * no se levanta contexto de Spring ni base de datos real.
 */
@ExtendWith(MockitoExtension.class)
class IntegratedScenarioTest {

    @Mock
    private RescueCaseRepository rescueCaseRepository;
    @Mock
    private RescueCaseMapper rescueCaseMapper;

    @Mock
    private AnimalRepository animalRepository;
    @Mock
    private SpecialistRepository specialistRepository;
    @Mock
    private TreatmentRepository treatmentRepository;
    @Mock
    private TreatmentMapper treatmentMapper;

    private RescueCaseServiceImpl rescueCaseService;
    private TreatmentServiceImpl treatmentService;

    private RescueCase rescueCase;
    private Animal animal;
    private Specialist specialist;

    @BeforeEach
    void setUp() {
        rescueCaseService = new RescueCaseServiceImpl(rescueCaseRepository, rescueCaseMapper);
        treatmentService = new TreatmentServiceImpl(
                animalRepository, specialistRepository, treatmentRepository, treatmentMapper);

        rescueCase = new RescueCase(
                "RES-2026-100", LocalDate.of(2026, 8, 20), "Santa Marta",
                RescueStatus.IN_REHABILITATION);

        animal = new Animal("AN-2026-100", "Green Sea Turtle", "Chelonia mydas", AnimalSex.MALE);
        rescueCase.assignAnimal(animal);

        specialist = new Specialist("SPEC-001", "Elena", "Vargas", "elena@deepblue.com");

        lenient().when(rescueCaseRepository.findByCaseCode("RES-2026-100"))
                .thenReturn(Optional.of(rescueCase));
        lenient().when(animalRepository.findByAnimalCode("AN-2026-100"))
                .thenReturn(Optional.of(animal));
        lenient().when(specialistRepository.findByProfessionalCode("SPEC-001"))
                .thenReturn(Optional.of(specialist));
        lenient().when(treatmentRepository.save(any(Treatment.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(treatmentMapper.toResponse(any(Treatment.class)))
                .thenReturn(new TreatmentResponse(
                        1L, "AN-2026-100", "SPEC-001",
                        LocalDateTime.of(2026, 8, 21, 9, 0),
                        TreatmentType.WOUND_CARE, "Cleaning of left front flipper injury."));
    }

    // 51. Solicitud válida
    @Test
    void shouldRegisterValidTreatment() {
        CreateTreatmentRequest request = new CreateTreatmentRequest(
                "AN-2026-100", "SPEC-001",
                LocalDateTime.of(2026, 8, 21, 9, 0),
                TreatmentType.WOUND_CARE,
                "Cleaning of left front flipper injury.");

        TreatmentResponse response = treatmentService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.type()).isEqualTo(TreatmentType.WOUND_CARE);
    }

    // 52. Solicitud inválida 1: fecha anterior al rescate
    @Test
    void shouldRejectTreatmentBeforeRescueDate() {
        CreateTreatmentRequest request = new CreateTreatmentRequest(
                "AN-2026-100", "SPEC-001",
                LocalDateTime.of(2026, 8, 15, 9, 0),
                TreatmentType.WOUND_CARE, "Too early.");

        assertThatThrownBy(() -> treatmentService.register(request))
                .isInstanceOf(BusinessRuleException.class);
    }

    // 53. Solicitud inválida 2: caso RELEASED
    @Test
    void shouldRejectTreatmentWhenCaseIsReleased() {
        when(rescueCaseRepository.save(rescueCase)).thenReturn(rescueCase);

        rescueCaseService.changeStatus("RES-2026-100",
                new ChangeRescueStatusRequest(RescueStatus.READY_FOR_RELEASE));
        rescueCaseService.changeStatus("RES-2026-100",
                new ChangeRescueStatusRequest(RescueStatus.RELEASED));

        assertThat(rescueCase.getStatus()).isEqualTo(RescueStatus.RELEASED);

        CreateTreatmentRequest request = new CreateTreatmentRequest(
                "AN-2026-100", "SPEC-001",
                LocalDateTime.of(2026, 8, 22, 9, 0),
                TreatmentType.OBSERVATION, "Post-release check.");

        assertThatThrownBy(() -> treatmentService.register(request))
                .isInstanceOf(BusinessRuleException.class);
    }

    // 54. Solicitud inválida 3: especialista desactivado
    @Test
    void shouldRejectTreatmentWhenSpecialistIsDeactivated() {
        ReflectionTestUtils.setField(specialist, "active", false);

        CreateTreatmentRequest request = new CreateTreatmentRequest(
                "AN-2026-100", "SPEC-001",
                LocalDateTime.of(2026, 8, 22, 9, 0),
                TreatmentType.WOUND_CARE, "Follow-up.");

        assertThatThrownBy(() -> treatmentService.register(request))
                .isInstanceOf(BusinessRuleException.class);
    }
}