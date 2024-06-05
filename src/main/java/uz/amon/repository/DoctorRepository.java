package uz.amon.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import uz.amon.domain.entity.Doctor;
import uz.amon.domain.enums.Speciality;

public interface DoctorRepository extends JpaRepository<Doctor,Long>  {
    boolean existsDoctorBySpeciality(String  speciality);
    @Query( nativeQuery = true ,value = "select d1_0.chat_id from doctor d1_0 where d1_0.speciality=?")
    Long getChatIdBySpeciality(String speciality);
}
