package uz.amon.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import uz.amon.domain.entity.Complaint;

public interface ComplaintRepository extends JpaRepository<Complaint,Long>
{
    @Query(value = "SELECT * FROM complaint ORDER BY created_at DESC LIMIT 1", nativeQuery = true)
    Complaint findLatestComplaint();
}
