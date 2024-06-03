package uz.amon.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import uz.amon.domain.entity.Patient;
import uz.amon.domain.enums.PatientState;

public interface PatientRepository extends JpaRepository<Patient, Long>
{

    @Query(value = "SELECT state FROM patient where chat_id=:chatId",nativeQuery = true)
    PatientState findPatientStateByChatId(Long chatId);

    Patient findByChatId(Long chatId);

    boolean existsByChatId(long chatId);
}
