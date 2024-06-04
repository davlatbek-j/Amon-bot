package uz.amon.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import uz.amon.domain.enums.DoctorState;

@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)

@Entity
public class Doctor
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    String firstname;

    String lastname;

    Long chatId;

    @Enumerated(EnumType.STRING)
    DoctorState state;

//    @OneToOne
//    Photo photo;

    public Doctor(String firstname, String lastname)
    {
        this.firstname = firstname;
        this.lastname = lastname;
    }

    Long currentComplaintId;

}
