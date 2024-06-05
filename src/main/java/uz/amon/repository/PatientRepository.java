package uz.amon.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.amon.domain.entity.Patient;

import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient,Long> {
    Optional<Patient> findByChatId(Long chatId);

    @Transactional
    @Modifying
    @Query(nativeQuery = true, value = " INSERT INTO patient ( chat_id) VALUES (?)")
    void saveChatId( Long chatId);
    @Query(nativeQuery = true,value = "SELECT p.id FROM Patient p WHERE p.chat_id = :chatId")
    Long findPatientIdByChatId(@Param("chatId") Long chatId);
}
