package uz.amon.aa;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import uz.amon.domain.entity.Doctor;
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
        doctor.setFirstname("Davlatbek");
        doctor.setLastname("Jamoliddinov");
        list.add(doctor);
        list.add(new Doctor("Anna","Stone"));
        list.add(new Doctor("Jenifer","Lopes"));
        list.add(new Doctor("Angelina","Jullie"));
        list.add(new Doctor("Megen","Fidock"));
        list.add(new Doctor("Ly","Basil"));
        list.add(new Doctor("Jammie","Kibby"));
        list.add(new Doctor("Dell","Menat"));

        doctorRepo.saveAll(list);
    }
}
