package uz.amon.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import uz.amon.domain.enums.PatientState;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)

@Entity
public class Patient
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(length = 50)
    String firstname;

    @Column(length = 50)
    String lastname;

    @Column(length = 50)
    String surname;

    @Column(unique=true)
    String phone;

    Long chatId;

    @Enumerated(EnumType.STRING)
    PatientState state;

    @OneToOne
    Complaint complaint;
}
