package uz.amon.domain.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import uz.amon.domain.entity.Complaint;
import uz.amon.domain.enums.PatientState;

@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PatientResponseDTO {

    Long id;

    String firstname;

    String lastname;

    String surname;

    String phone;

    Long chatId;

    PatientState state;

    Complaint complaint;

}
