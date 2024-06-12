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

//        Doctor doctor = new Doctor();
//        doctor.setChatId(104800996L);
//        doctor.setSpecialityRu("Невропатолог");
//        doctor.setSpecialityUz("Nevropatolog");
//        doctor.setState(DoctorState.START);


        Doctor doctor3 = new Doctor();
        doctor3.setChatId(104800996L);
        doctor3.setSpecialityRu("Педиатр-Невропатолог");
        doctor3.setSpecialityUz("Pediatr-Nevropatolog");
        doctor3.setState(DoctorState.START);

        if (!doctorRepo.existsByChatId(doctor3.getChatId()))
            doctorRepo.save(doctor3);


        Doctor doctor2 = new Doctor();
        doctor2.setChatId(1762041853L);
        doctor2.setSpecialityRu("Кардиолог");
        doctor2.setSpecialityUz("Kardiolog");
        doctor2.setState(DoctorState.START);


        if (!doctorRepo.existsByChatId(doctor2.getChatId()))
            doctorRepo.save(doctor2);

    }

}
