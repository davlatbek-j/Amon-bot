package uz.amon.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.amon.domain.entity.Photo;

import java.util.Optional;

public interface PhotoRepository extends JpaRepository<Photo,Long> {
    Optional<Photo> findByHttpUrl(String httpUrl);
}
