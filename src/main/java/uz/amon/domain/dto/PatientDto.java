package uz.amon.domain.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import uz.amon.domain.entity.Complaint;
import uz.amon.domain.enums.PatientState;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PatientDto {
    Long id;
    String firstname;
    String lastname;
    String phone;
    Long chatId;
    PatientState state;
    Set<Complaint> complaint;
}
