package uz.amon.domain.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import uz.amon.domain.entity.Photo;
import uz.amon.domain.enums.ComplaintStatus;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ComplaintDto {
    Long id;
    String photoUrl;

    String message;

    Long writerId;

    Long doctorId;
    ComplaintStatus status;

    String answerOfDoctor;
}
