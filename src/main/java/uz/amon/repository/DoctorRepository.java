package uz.amon.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.amon.domain.entity.Doctor;
import uz.amon.domain.enums.DoctorState;

public interface DoctorRepository extends JpaRepository<Doctor, Long>
{

    @Query(value = "SELECT state from doctor  where chat_id=:chatId",nativeQuery = true)
    DoctorState findStateByChatId(long chatId);

    boolean existsByChatId(long chatId);

    Doctor findByChatId(long chatId);

    @Query(value = "SELECT last_session_number FROM doctor where id= :doctorId", nativeQuery = true)
    Integer findLastSessionNumberById(@Param("doctorId") Long doctorId);

}
