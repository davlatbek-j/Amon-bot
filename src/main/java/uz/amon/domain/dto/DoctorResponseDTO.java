package uz.amon.domain.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import uz.amon.domain.enums.DoctorState;

@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DoctorResponseDTO {

    Long id;

    String firstname;

    String lastname;

    Long chatId;

    DoctorState state;

    Long currentComplaintId;

}
