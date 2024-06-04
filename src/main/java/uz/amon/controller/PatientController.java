package uz.amon.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uz.amon.domain.dto.ApiResponse;
import uz.amon.domain.dto.PatientCreateDTO;
import uz.amon.domain.dto.PatientResponseDTO;
import uz.amon.domain.dto.PatientUpdateDTO;
import uz.amon.service.PatientService;

import java.util.List;

@RestController
@RequestMapping("/patient")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<PatientResponseDTO>> create(
            @RequestBody PatientCreateDTO patientCreateDTO
    ) {
        return ResponseEntity.ok(patientService.save(patientCreateDTO));
    }

    @GetMapping("/findAll")
    public ResponseEntity<ApiResponse<List<PatientResponseDTO>>> findAll() {
        return ResponseEntity.ok(patientService.findAll());
    }

    @GetMapping("/find/{id}")
    public ResponseEntity<ApiResponse<PatientResponseDTO>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(patientService.findById(id));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<ApiResponse<PatientResponseDTO>> update(
            @PathVariable Long id,
            @RequestBody PatientUpdateDTO patientUpdateDTO
    ) {
        return ResponseEntity.ok(patientService.update(patientUpdateDTO, id));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        patientService.delete(id);
        return ResponseEntity.status(HttpStatus.valueOf(204)).build();
    }

}
