package uz.amon.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import uz.amon.domain.enums.PatientState;

import java.util.Set;


@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Getter
@Setter
@Table(name = "Patient", uniqueConstraints = {@UniqueConstraint(columnNames = "chat_id")})
public class Patient
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    String firstname;

    String lastname;

    @Column(unique=true)
    String phone;

    @Column(name = "chat_id", nullable = false, unique = true)
    Long chatId;

    @Enumerated(EnumType.STRING)
    PatientState state;

    @OneToMany
    Set<Complaint> complaint;
}
