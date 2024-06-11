package uz.amon.aa;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import uz.amon.domain.entity.Doctor;
import uz.amon.domain.enums.DoctorState;
import uz.amon.repository.DoctorRepository;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner
{
    private final DoctorRepository doctorRepo;

    @Override
    public void run(String... args) throws Exception
    {
        List<Doctor> list = new ArrayList<>();

        Doctor doctor = new Doctor();
        doctor.setChatId(1762041853L);
        doctor.setSpeciality("Невропатолог");
        doctor.setState(DoctorState.START);

        Doctor doctor2 = new Doctor();
        doctor2.setChatId(5256030505L);
        doctor2.setSpeciality("Кардиолог");
        doctor2.setState(DoctorState.START);

        list.add(doctor);
        list.add(doctor2);

        Doctor doctor3 = new Doctor("Aйбек", "Педиатр");
//        doctor3.setChatId(6936302773L);
        doctor3.setState(DoctorState.START);
        doctor3.setSpeciality("Педиатр");

        list.add(doctor3);

        doctorRepo.saveAll(list);
    }

}
