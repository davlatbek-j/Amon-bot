package uz.amon.mapper;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uz.amon.domain.dto.ComplaintDto;
import uz.amon.domain.entity.Complaint;
import uz.amon.repository.ComplaintRepository;

@Component
@AllArgsConstructor
public class ComplaintMapper {

    private final ComplaintRepository complaintRepository;

   /* public Complaint toEntity(ComplaintDto dto) {
        Complaint complaint = new Complaint();
        complaint.setMessage(dto.getMessage());
        complaint.setDoctorId(dto.getDoctorId());

    }
*/
}
