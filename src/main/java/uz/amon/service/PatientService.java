package uz.amon.service;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import uz.amon.domain.dto.ApiResponse;
import uz.amon.domain.dto.PatientCreateDTO;
import uz.amon.domain.dto.PatientResponseDTO;
import uz.amon.domain.dto.PatientUpdateDTO;
import uz.amon.domain.entity.Patient;
import uz.amon.domain.enums.PatientState;
import uz.amon.repository.PatientRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;

    private final ModelMapper modelMapper;

    public ApiResponse<PatientResponseDTO> save(PatientCreateDTO patientCreateDTO) {
        if (patientRepository.findByPhone(patientCreateDTO.getPhone()).isPresent()) {
            return new ApiResponse<>(null, "This phone number is already exists");
        }
        Patient patient = modelMapper.map(patientCreateDTO, Patient.class);
        patient = patientRepository.save(patient);
        PatientResponseDTO responseDTO = modelMapper.map(patient, PatientResponseDTO.class);
        return new ApiResponse<>(responseDTO, "success");
    }

    public void delete(Long id) {
        patientRepository.deleteById(id);
    }

    public ApiResponse<PatientResponseDTO> update(PatientUpdateDTO patientUpdateDTO, Long id) {
        Optional<Patient> optionalPatient = patientRepository.findById(id);
        if (optionalPatient.isEmpty()) {
            return new ApiResponse<>(null, "Patient is not found with this id");
        }
        Patient patient = optionalPatient.get();
        patient.setFirstname(patientUpdateDTO.getFirstname());
        patient.setLastname(patientUpdateDTO.getLastname());
        patient.setPhone(patientUpdateDTO.getPhone());
        PatientResponseDTO responseDTO = modelMapper.map(patientRepository.save(patient), PatientResponseDTO.class);
        return new ApiResponse<>(responseDTO, "success");
    }

    public ApiResponse<PatientResponseDTO> findById(Long id) {
        Optional<Patient> optionalPatient = patientRepository.findById(id);
        if (optionalPatient.isEmpty()) {
            return new ApiResponse<>(null, "Patient is not found with this id");
        }
        Patient patient = optionalPatient.get();
        PatientResponseDTO responseDTO = modelMapper.map(patient, PatientResponseDTO.class);
        return new ApiResponse<>(responseDTO, "success");
    }

    public ApiResponse<List<PatientResponseDTO>> findAll() {
        List<PatientResponseDTO> list = patientRepository.findAll().stream()
                .map(patient -> {
                    return modelMapper.map(patient, PatientResponseDTO.class);
                }).toList();
        return new ApiResponse<>(list, "success");
    }

    public ApiResponse<PatientState> updateState(Long chatId, PatientState state) {
        Patient patient = patientRepository.findByChatId(chatId);
        if (patient==null) {
            return new ApiResponse<>(null, "Patient is not found with this chatId");
        }
        patient.setState(state);
        patientRepository.save(patient);
        return new ApiResponse<>(state, "success");
    }


}
