package uz.amon.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)

@Entity
public class PatientSessionAndThemeNumber
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    Long patientId;

    Long doctorId;

    Integer sessionNumber;

    Integer lastThemeNumber;
}


