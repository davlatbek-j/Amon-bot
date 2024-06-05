package uz.amon.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Photo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    String name;

    String systemPath;

    String httpUrl;

    String type;
    public Photo(String name, String systemPath, String httpUrl, String type) {
        this.name = name;
        this.systemPath = systemPath;
        this.httpUrl = httpUrl;
        this.type = type;
    }

}
