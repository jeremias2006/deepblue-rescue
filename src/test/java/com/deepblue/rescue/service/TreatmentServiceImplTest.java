package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.AnimalSex;
import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.domain.Specialist;
import com.deepblue.rescue.domain.Treatment;
import com.deepblue.rescue.domain.TreatmentType;
import com.deepblue.rescue.dto.request.CreateTreatmentRequest;
import com.deepblue.rescue.dto.response.TreatmentResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.TreatmentMapper;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.repository.SpecialistRepository;
import com.deepblue.rescue.repository.TreatmentRepository;
import com.deepblue.rescue.service.impl.TreatmentServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TreatmentServiceImplTest {

    @Mock
    private AnimalRepository animalRepository;

    @Mock
    private SpecialistRepository specialistRepository;

    @Mock
    private TreatmentRepository treatmentRepository;

    @Mock
    private TreatmentMapper mapper;

    @InjectMocks
    private TreatmentServiceImpl service;

    private static final LocalDate RESCUE_DATE = LocalDate.of(2026, 8, 20);
    private static final LocalDateTime VALID_DATE = LocalDateTime.of(2026, 8, 21, 9, 0);

    private Animal newAnimal(RescueStatus status) {
        RescueCase rescueCase = new RescueCase("RES-001", RESCUE_DATE, "Santa Marta", status);
        Animal animal = new Animal("AN-001", "Green Sea Turtle", "Chelonia mydas", AnimalSex.MALE);
        rescueCase.assignAnimal(animal);
        return animal;
    }

    private Specialist newSpecialist(boolean active) {
        Specialist specialist = new Specialist(
                "SPEC-001", "Elena", "Vargas", "elena@deepblue.com");
        ReflectionTestUtils.setField(specialist, "active", active);
        return specialist;
    }

    private CreateTreatmentRequest newRequest(LocalDateTime performedAt) {
        return new CreateTreatmentRequest(
                "AN-001", "SPEC-001", performedAt,
                TreatmentType.WOUND_CARE, "Cleaning of left front flipper injury.");
    }

    // TEST 5
    @Test
    void shouldRegisterTreatment() {
        Animal animal = newAnimal(RescueStatus.IN_REHABILITATION);
        Specialist specialist = newSpecialist(true);
        TreatmentResponse response = new TreatmentResponse(
                1L, "AN-001", "SPEC-001", VALID_DATE,
                TreatmentType.WOUND_CARE, "Cleaning of left front flipper injury.");

        when(animalRepository.findByAnimalCode("AN-001")).thenReturn(Optional.of(animal));
        when(specialistRepository.findByProfessionalCode("SPEC-001")).thenReturn(Optional.of(specialist));
        when(treatmentRepository.save(any(Treatment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any(Treatment.class))).thenReturn(response);

        TreatmentResponse result = service.register(newRequest(VALID_DATE));

        assertThat(result).isEqualTo(response);
        verify(treatmentRepository).save(any(Treatment.class));
    }

    // TEST 6
    @Test
    void shouldRejectInactiveSpecialist() {
        Animal animal = newAnimal(RescueStatus.IN_REHABILITATION);
        Specialist specialist = newSpecialist(false);

        when(animalRepository.findByAnimalCode("AN-001")).thenReturn(Optional.of(animal));
        when(specialistRepository.findByProfessionalCode("SPEC-001")).thenReturn(Optional.of(specialist));

        assertThatThrownBy(() -> service.register(newRequest(VALID_DATE)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("inactive");

        verify(treatmentRepository, never()).save(any());
    }

    // TEST 7
    @Test
    void shouldRejectTreatmentWhenCaseIsReleased() {
        Animal animal = newAnimal(RescueStatus.RELEASED);
        Specialist specialist = newSpecialist(true);

        when(animalRepository.findByAnimalCode("AN-001")).thenReturn(Optional.of(animal));
        when(specialistRepository.findByProfessionalCode("SPEC-001")).thenReturn(Optional.of(specialist));

        assertThatThrownBy(() -> service.register(newRequest(VALID_DATE)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("RELEASED");

        verify(treatmentRepository, never()).save(any());
    }

    // Extra: Regla 1
    @Test
    void shouldThrowWhenAnimalNotFound() {
        when(animalRepository.findByAnimalCode("AN-001")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.register(newRequest(VALID_DATE)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("AN-001");

        verify(treatmentRepository, never()).save(any());
    }

    // Extra: Regla 5
    @Test
    void shouldRejectTreatmentBeforeRescueDate() {
        Animal animal = newAnimal(RescueStatus.IN_REHABILITATION);
        Specialist specialist = newSpecialist(true);

        when(animalRepository.findByAnimalCode("AN-001")).thenReturn(Optional.of(animal));
        when(specialistRepository.findByProfessionalCode("SPEC-001")).thenReturn(Optional.of(specialist));

        LocalDateTime tooEarly = LocalDateTime.of(2026, 8, 15, 9, 0);

        assertThatThrownBy(() -> service.register(newRequest(tooEarly)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("before the rescue date");

        verify(treatmentRepository, never()).save(any());
    }
}