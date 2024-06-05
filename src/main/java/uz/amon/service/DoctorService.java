package uz.amon.service;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import uz.amon.domain.dto.ApiResponse;
import uz.amon.domain.dto.DoctorCreateDTO;
import uz.amon.domain.dto.DoctorResponseDTO;
import uz.amon.domain.entity.Doctor;
import uz.amon.domain.enums.DoctorState;
import uz.amon.repository.DoctorRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor

@Service
public class DoctorService {

    private final DoctorRepository doctorRepo;

    private final ModelMapper modelMapper;

    public boolean isDoctor(long chatId) {
        return doctorRepo.existsByChatId(chatId);
    }

    public DoctorState getDoctorState(long chatId) {
        return null;
    }

    public List<Doctor> getList() {
        return doctorRepo.findAll();
    }

    public ApiResponse<DoctorResponseDTO> save(DoctorCreateDTO doctorCreateDTO) {
        Doctor doctor = modelMapper.map(doctorCreateDTO, Doctor.class);
        DoctorResponseDTO responseDTO = modelMapper.map(doctorRepo.save(doctor), DoctorResponseDTO.class);
        return new ApiResponse<>(responseDTO, "success");
    }

    public void delete(Long id) {
        doctorRepo.deleteById(id);
    }

    public ApiResponse<DoctorResponseDTO> update(Long id, String firstName, String lastName) {
        Optional<Doctor> optionalDoctor = doctorRepo.findById(id);
        if (optionalDoctor.isEmpty()) {
            return new ApiResponse<>(null, "Doctor is not found with this id");
        }
        Doctor doctor = optionalDoctor.get();
        doctor.setFirstname(firstName);
        doctor.setLastname(lastName);
        DoctorResponseDTO responseDTO = modelMapper.map(doctorRepo.save(doctor), DoctorResponseDTO.class);
        return new ApiResponse<>(responseDTO, "success");
    }

    public ApiResponse<DoctorResponseDTO> findById(Long id) {
        Optional<Doctor> optionalDoctor = doctorRepo.findById(id);
        if (optionalDoctor.isEmpty()) {
            return new ApiResponse<>(null, "Doctor is not found with this id");
        }
        Doctor doctor = optionalDoctor.get();
        DoctorResponseDTO responseDTO = modelMapper.map(doctor, DoctorResponseDTO.class);
        return new ApiResponse<>(responseDTO, "success");
    }

    public ApiResponse<List<DoctorResponseDTO>> findByAll() {
        List<DoctorResponseDTO> list = doctorRepo.findAll().stream()
                .map(doctor -> {
                    return modelMapper.map(doctor, DoctorResponseDTO.class);
                }).toList();
        return new ApiResponse<>(list, "success");
    }
}
