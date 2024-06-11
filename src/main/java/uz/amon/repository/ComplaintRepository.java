package uz.amon.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.amon.domain.entity.Complaint;

public interface ComplaintRepository extends JpaRepository<Complaint,Long>
{
    Integer countByWriterId(Long id);

    Integer countByDoctorId(Long id);

    Integer countByDoctorIdAndWriterId(Long doctorId, Long writerId);

    @Query(value = "SELECT COUNT(doctor_id) FROM complaint WHERE writer_id = :writerId", nativeQuery = true)
    Long countDoctorIdByWriterId(@Param("writerId") Long writerId);

    boolean existsByWriterId(Long writerId);

    @Query(value = "SELECT complaint.session_num FROM complaint where writer_id= :writerId LIMIT 1", nativeQuery = true)
    Integer findSessionNumByWriterId(@Param("writerId") Long writerId);

    boolean existsByDoctorIdAndWriterId(Long doctorId, Long writerId);
}
