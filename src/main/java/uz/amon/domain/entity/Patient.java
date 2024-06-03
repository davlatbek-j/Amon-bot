package uz.amon.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import uz.amon.domain.enums.PatientLanguage;
import uz.amon.domain.enums.PatientState;

@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)

@Entity
public class Patient
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    String firstname;

    String lastname;

    @Column(unique=true)
    String phone;

    Long chatId;

    @Enumerated(EnumType.STRING)
    PatientState state;

    @Enumerated(EnumType.STRING)
    PatientLanguage language;

    @OneToOne(cascade=CascadeType.ALL)
    Complaint complaint;
}
