package uz.amon.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import uz.amon.domain.enums.ComplaintStatus;

@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)

@Entity
public class Complaint
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @OneToOne
    Photo photo;

    @Column(length = 500)
    String message;

    Long writerId;

    Long doctorId;

    @Enumerated(EnumType.STRING)
    ComplaintStatus status;

    @Column(length = 500)
    String answerOfDoctor;
}
