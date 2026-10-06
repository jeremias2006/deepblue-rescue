package com.deepblue.rescue.controller;

import com.deepblue.rescue.domain.AnimalSex;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.domain.TreatmentType;
import com.deepblue.rescue.dto.response.AnimalResponse;
import com.deepblue.rescue.dto.response.TreatmentResponse;
import com.deepblue.rescue.exception.GlobalExceptionHandler;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.service.AnimalService;
import com.deepblue.rescue.service.TreatmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnimalController.class)
@Import(GlobalExceptionHandler.class)
class AnimalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnimalService animalService;

    @MockitoBean
    private TreatmentService treatmentService;

    private AnimalResponse newAnimal(String animalCode) {
        return new AnimalResponse(
                1L,
                animalCode,
                "Green Sea Turtle",
                "Chelonia mydas",
                AnimalSex.FEMALE,
                "RES-2026-001",
                RescueStatus.IN_REHABILITATION
        );
    }

    // Caso 12: GET animal -> 200
    @Test
    void shouldReturnAnimalByCode() throws Exception {
        when(animalService.findByCode("AN-001")).thenReturn(newAnimal("AN-001"));

        mockMvc.perform(get("/api/animals/{code}", "AN-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.animalCode").value("AN-001"))
                .andExpect(jsonPath("$.commonName").value("Green Sea Turtle"))
                .andExpect(jsonPath("$.rescueStatus").value("IN_REHABILITATION"));

        verify(animalService).findByCode("AN-001");
    }

    // Caso 16: GET animal inexistente -> 404 + ErrorResponse
    @Test
    void shouldReturn404WhenAnimalDoesNotExist() throws Exception {
        when(animalService.findByCode("AN-999"))
                .thenThrow(new ResourceNotFoundException("Animal not found: AN-999"));

        mockMvc.perform(get("/api/animals/{code}", "AN-999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Animal not found: AN-999"))
                .andExpect(jsonPath("$.details").isMap());
    }

    // Caso 13: GET animales en rehabilitación -> 200 (y la ruta NO se confunde con /{animalCode})
    @Test
    void shouldReturnAnimalsInRehabilitation() throws Exception {
        when(animalService.findAnimalsInRehabilitation())
                .thenReturn(List.of(newAnimal("AN-001"), newAnimal("AN-002")));

        mockMvc.perform(get("/api/animals/in-rehabilitation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].animalCode").value("AN-001"))
                .andExpect(jsonPath("$[1].animalCode").value("AN-002"));

        verify(animalService).findAnimalsInRehabilitation();
        verify(animalService, never()).findByCode(any());
    }

    // Caso 14: GET tratamientos del animal -> 200
    @Test
    void shouldReturnAnimalTreatments() throws Exception {
        TreatmentResponse treatment = new TreatmentResponse(
                100L,
                "AN-001",
                "SPEC-001",
                LocalDateTime.of(2026, 8, 21, 9, 0),
                TreatmentType.WOUND_CARE,
                "Cleaning and treatment of flipper injury."
        );

        when(treatmentService.findByAnimalCode("AN-001")).thenReturn(List.of(treatment));

        mockMvc.perform(get("/api/animals/{code}/treatments", "AN-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].animalCode").value("AN-001"))
                .andExpect(jsonPath("$[0].type").value("WOUND_CARE"));

        verify(treatmentService).findByAnimalCode("AN-001");
    }

    // Caso 15: GET eligibility -> 200
    @Test
    void shouldReturnTreatmentEligibility() throws Exception {
        when(animalService.canReceiveTreatment("AN-001")).thenReturn(true);

        mockMvc.perform(get("/api/animals/{code}/treatment-eligibility", "AN-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.animalCode").value("AN-001"))
                .andExpect(jsonPath("$.eligible").value(true));

        verify(animalService).canReceiveTreatment("AN-001");
    }

    // Extra: eligibility = false sigue siendo 200 (no es un error)
    @Test
    void shouldReturnNotEligibleWhenAnimalCannotReceiveTreatment() throws Exception {
        when(animalService.canReceiveTreatment("AN-002")).thenReturn(false);

        mockMvc.perform(get("/api/animals/{code}/treatment-eligibility", "AN-002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.animalCode").value("AN-002"))
                .andExpect(jsonPath("$.eligible").value(false));
    }

    // Reto del estudiante: eligibility de animal inexistente -> 404
    @Test
    void shouldReturn404WhenEligibilityAnimalDoesNotExist() throws Exception {
        when(animalService.canReceiveTreatment("AN-999"))
                .thenThrow(new ResourceNotFoundException("Animal not found: AN-999"));

        mockMvc.perform(get("/api/animals/{code}/treatment-eligibility", "AN-999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Animal not found: AN-999"))
                .andExpect(jsonPath("$.details").isMap());

        verify(animalService).canReceiveTreatment("AN-999");
    }
}
