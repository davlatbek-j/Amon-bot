package uz.amon.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.amon.domain.entity.Photo;

public interface PhotoRepository extends JpaRepository<Photo,Long>
{

}
