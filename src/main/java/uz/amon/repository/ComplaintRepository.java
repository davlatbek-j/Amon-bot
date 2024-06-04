package uz.amon.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.amon.domain.entity.Complaint;

public interface ComplaintRepository extends JpaRepository<Complaint,Long>
{

}
