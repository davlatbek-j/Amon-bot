package uz.amon.domain.entity;

import jakarta.persistence.*;
import jakarta.websocket.OnMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.amon.domain.enums.PatientState;
import uz.amon.domain.enums.Speciality;

import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Doctor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    Long chatId;

    @Enumerated(EnumType.STRING)
    PatientState state;

    String speciality;

}
