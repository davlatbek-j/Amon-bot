package uz.amon.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.amon.domain.entity.Complaint;
import uz.amon.domain.entity.Patient;
import uz.amon.domain.enums.ComplaintStatus;
import uz.amon.domain.enums.PatientState;
import uz.amon.domain.model.ApiResponse;
import uz.amon.repository.ComplaintRepository;
import uz.amon.repository.PatientRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ComplaintService {
    private final ComplaintRepository complaintRepository;
    /*public void saveComplaint(String chatId) {
        complaintRepository.saveComplaintWithWriterId(Long.valueOf(chatId));
    }*/
    public ResponseEntity<ApiResponse> updateComplaintStatus(Long writerId, ComplaintStatus status) {
        Optional<Complaint> optionalComplaint = complaintRepository.findByWriterId(writerId);
        if (optionalComplaint.isPresent()) {
            Complaint complaint = optionalComplaint.get();
            complaint.setStatus(status);
            complaintRepository.save(complaint);
            return ResponseEntity.ok().body(new ApiResponse(200, "Success", "state: " + complaint.getStatus()));
        }
        return ResponseEntity.notFound().build();
    }


}
