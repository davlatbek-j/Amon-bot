package uz.amon.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.amon.domain.entity.PatientSessionAndThemeNumber;

public interface PatientSessionNumberRepo extends JpaRepository<PatientSessionAndThemeNumber, Long>
{
    PatientSessionAndThemeNumber findByDoctorId(Long doctorId);

    PatientSessionAndThemeNumber findByPatientId(Long patientId);

    boolean existsByDoctorIdAndPatientId(Long doctorId, Long patientId);


    PatientSessionAndThemeNumber findByPatientIdAndDoctorId(Long patientId, Long doctorId);

}
