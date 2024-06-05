package uz.amon.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import uz.amon.domain.entity.Patient;
import uz.amon.domain.enums.PatientState;
import uz.amon.domain.model.ApiResponse;
import uz.amon.repository.PatientRepository;

import java.util.Optional;
@Service
@RequiredArgsConstructor
@Transactional
public class PatientService {
    private final PatientRepository patientRepository;
    public Optional<Patient> findByChatId(Long chatId) {
        return patientRepository.findByChatId(chatId);
    }

    public ResponseEntity<ApiResponse> updateState(Long chatId, PatientState patientState) {
        Optional<Patient> optionalPatient = patientRepository.findByChatId(chatId);
        if (optionalPatient.isPresent()) {
            Patient patient = optionalPatient.get();
            patient.setState(patientState);
            patientRepository.save(patient);
            return ResponseEntity.ok().body(new ApiResponse(200, "Success", "state: " + patient.getState()));
        }
        return ResponseEntity.notFound().build();
    }
    public void saveChatId( String chatId) {
        patientRepository.saveChatId(Long.valueOf(chatId));
    }

}
