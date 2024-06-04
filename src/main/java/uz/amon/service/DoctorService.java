package uz.amon.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uz.amon.domain.entity.Doctor;
import uz.amon.domain.enums.DoctorState;
import uz.amon.repository.DoctorRepository;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor

@Service
public class DoctorService
{

    private final DoctorRepository doctorRepo;

    public boolean isDoctor(long chatId) {
       return doctorRepo.existsByChatId(chatId);
    }

    public DoctorState getDoctorState(long chatId)
    {
        return null;
    }

    public List<Doctor> getList()
    {
        return doctorRepo.findAll();
    }
}
