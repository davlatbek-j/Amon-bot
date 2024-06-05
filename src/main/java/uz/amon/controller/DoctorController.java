package uz.amon.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uz.amon.domain.dto.ApiResponse;
import uz.amon.domain.dto.DoctorCreateDTO;
import uz.amon.domain.dto.DoctorResponseDTO;
import uz.amon.service.DoctorService;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorService doctorService;

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<DoctorResponseDTO>> create(@RequestBody DoctorCreateDTO doctorCreateDTO) {
        return ResponseEntity.ok(doctorService.save(doctorCreateDTO));
    }

    @GetMapping("/findAll")
    public ResponseEntity<ApiResponse<List<DoctorResponseDTO>>> findAll() {
        return ResponseEntity.ok(doctorService.findByAll());
    }

    @GetMapping("/find/{id}")
    public ResponseEntity<ApiResponse<DoctorResponseDTO>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(doctorService.findById(id));
    }

    @PutMapping("/delete/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        doctorService.delete(id);
        return ResponseEntity.status(HttpStatus.valueOf(204)).build();
    }

    @DeleteMapping("/update/{id}")
    public ResponseEntity<ApiResponse<DoctorResponseDTO>> update(
            @RequestParam String firstName,
            @RequestParam String lastName,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(doctorService.update(id, firstName, lastName));
    }

}
