package uz.amon.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import uz.amon.domain.entity.Complaint;

import java.util.Optional;

public interface ComplaintRepository extends JpaRepository<Complaint,Long> {

        @Modifying
        @Transactional
        @Query(value = "INSERT INTO Complaint (writer_id) SELECT p.id FROM Patient p WHERE p.chat_id =:chatId", nativeQuery = true)
        void saveComplaintWithWriterId(@Param("chatId") Long chatId);


    Optional<Complaint> findByWriterId(Long writerId);
    //Complaint findByWriterId1(String writerId);

   /* @Query(nativeQuery = true,value = "SELECT id FROM Complaint WHERE writer_id = ?")
    Long getIdByWriterId(Long writerId);
    */
    Complaint findComplaintByWriterId(Long writerId);


}
