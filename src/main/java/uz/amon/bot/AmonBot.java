package uz.amon.bot;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.amon.domain.dto.ComplaintDto;
import uz.amon.domain.dto.PatientDto;
import uz.amon.domain.entity.Complaint;
import uz.amon.domain.entity.Patient;
import uz.amon.domain.entity.Photo;
import uz.amon.domain.enums.ComplaintStatus;
import uz.amon.domain.enums.PatientState;
import uz.amon.domain.enums.Speciality;
import uz.amon.repository.ComplaintRepository;
import uz.amon.repository.DoctorRepository;
import uz.amon.repository.PatientRepository;
import uz.amon.service.ComplaintService;
import uz.amon.service.PatientService;
import uz.amon.service.PhotoService;
import uz.amon.service.ValidationService;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

import static uz.amon.domain.enums.PatientState.PHOTO;

@Component
@RequiredArgsConstructor
public class AmonBot extends TelegramLongPollingBot {
    private final PatientService patientService;
    private final PatientRepository patientRepository;
    private final BotService botService;
    private final ValidationService validationService;
    private final DoctorRepository doctorRepository;
    private final PhotoService photoService;
    private final ComplaintService complaintService;
    private final ComplaintRepository complaintRepository;

    private HashMap<Long, PatientState> patientStateHashMap = new HashMap<>();
    private HashMap<Long, ComplaintStatus> complaintStatusHashMap = new HashMap<>();

    private HashMap<Long, String> photoFileIds = new HashMap<>();
    private HashMap<Long, PatientDto> patientsList = new HashMap<>();
    private HashMap<Long, ComplaintDto> complaintList = new HashMap<>();

    private PatientState patientState = null;
    private Long doctorChatId;

    @Value("${photoUploadDir}")
    public String imgLocation;

    @Override
    public String getBotUsername() {
        return "https://t.me/amon_1_bot";
    }

    @Override
    public String getBotToken() {
        return "7307525379:AAHlhlZoBlvo5w5-Y_A7oyS7n4wMiasXOXA";
    }

    @SneakyThrows
    @Override
    public void onUpdateReceived(Update update) {


        if (update.hasMessage()) {
            Message message = update.getMessage();
            String text = message.getText();
            Long chatId = message.getChatId();


            if (Objects.equals(text, "/start")) {

                patientService.saveChatId(String.valueOf(chatId));
               // complaintService.saveComplaint(chatId.toString());
                patientStateHashMap.put(chatId, PatientState.START);
                patientService.updateState(chatId, PatientState.START);
            } else if (patientStateHashMap.get(chatId) == null || patientStateHashMap.get(chatId) == PatientState.DEFAULT) {
                patientStateHashMap.put(chatId, PatientState.DEFAULT);
                execute(botService.sendStart(chatId.toString()));
            }
            Optional<Patient> currentPatient = patientService.findByChatId(chatId);
            if (currentPatient.isPresent()) {
                patientState = currentPatient.get().getState();
                Long patientId = currentPatient.get().getId();
                switch (patientState) {
                    case START -> {

                        execute(botService.enterPhone(chatId.toString()));
                        patientService.updateState(chatId, PatientState.PHONE);
                        patientStateHashMap.put(chatId, PatientState.PHONE);


                    }
                    case FIRSTNAME -> {
                        String validationMessage = validationService.validateNames(text, "Ism");
                        if (validationMessage == null) {
                            String firstName = text;
                            execute(botService.enterLastName(chatId.toString()));
                            patientService.updateState(chatId, PatientState.LASTNAME);
                            patientStateHashMap.put(chatId, PatientState.LASTNAME);
                            PatientDto patientDTO = patientsList.get(chatId);
                            patientDTO.setFirstname(firstName);
                            patientsList.put(chatId, patientDTO);
                            System.out.println(patientDTO);
                        } else {
                            execute(botService.validationMessage(chatId.toString(), validationMessage));
                        }
                    }
                    case LASTNAME -> {
                        String validationMessage = validationService.validateNames(text, "Familya");
                        if (validationMessage == null) {
                            String lastName = text;
                            execute(botService.enterComplaintMessage(chatId.toString()));
                            patientService.updateState(chatId, PatientState.COMPLAINT_MESSAGE);
                            patientStateHashMap.put(chatId, PatientState.COMPLAINT_MESSAGE);
                            PatientDto patientDTO = patientsList.get(chatId);
                            patientDTO.setLastname(lastName);
                            patientsList.put(chatId, patientDTO);
                        } else {
                            execute(botService.validationMessage(chatId.toString(), validationMessage));
                        }


                    }
                    case PHONE -> {
                        String validationMessage = validationService.validatePhoneNumber(text);
                        if (validationMessage == null) {
                            String phone = text;
                            execute(botService.enterFirstname(chatId.toString()));
                            patientService.updateState(chatId, PatientState.FIRSTNAME);
                            patientStateHashMap.put(chatId, PatientState.FIRSTNAME);
                            PatientDto patientDto = patientsList.get(chatId);
                            if (patientDto == null) {
                                patientsList.put(chatId,PatientDto.builder().chatId(chatId).phone(phone).build());
                            }else {
                                patientDto.setPhone(phone);

                                patientsList.put(chatId, patientDto);
                            }

                        } else {
                            execute(botService.validationMessage(chatId.toString(), validationMessage));
                        }

                    }
                    case COMPLAINT_MESSAGE -> {
                        String complaintMessage = text;
                        execute(botService.sendPhoto(chatId.toString()));
                        patientService.updateState(chatId, PHOTO);
                        patientStateHashMap.put(chatId, PHOTO);
                        complaintRepository.saveComplaintWithWriterId(chatId);
                        complaintStatusHashMap.put(patientId, ComplaintStatus.NOT_CREATED);
                        complaintService.updateComplaintStatus(patientId, ComplaintStatus.NOT_CREATED);
                        ComplaintDto complaintDto = complaintList.get(patientId);
                        if (complaintDto==null){
                            complaintList.put(patientId,ComplaintDto.builder().writerId(patientId).message(complaintMessage).build());
                        }else {
                            complaintDto.setMessage(complaintMessage);
                            complaintList.put(patientRepository.findPatientIdByChatId(chatId), complaintDto);
                        }


                    }


                    case PHOTO -> {
                        if (message.hasPhoto()) {
                            System.out.println("if ga tushdimi");
                            String fileId = message.getPhoto().stream()
                                    .max(Comparator.comparing(PhotoSize::getFileSize))
                                    .orElseThrow(() -> new IllegalStateException("Photo is missing"))
                                    .getFileId();
                            GetFile getFileMethod = new GetFile(fileId);
                            File file = execute(getFileMethod);
                            String fileId1 = file.getFileId();
                            photoFileIds.put(chatId, fileId1);
                            String filePath = file.getFilePath();
                            String fileName = UUID.randomUUID() + Paths.get(filePath).getFileName().toString();
                            String fileUrl = "https://api.telegram.org/file/bot" + getBotToken() + "/" + filePath;
                            URL url = new URL(fileUrl);
                            System.out.println(url);
                            InputStream input = url.openStream();
                            String targetPath = imgLocation + fileName;
                            Files.copy(input, Paths.get(targetPath), StandardCopyOption.REPLACE_EXISTING);
                            input.close();
                            ComplaintDto complaintDto = complaintList.get(patientId);
                            Complaint complaint = complaintRepository.findComplaintByWriterId(patientId);

                            Photo photo = photoService.savePhotoFromTelegram(targetPath, String.valueOf(update.getMessage().getChatId()));
                            complaint.setPhoto(photo);
                            // doctorService.saveToDb(doctor);

                            String httpUrl = photo.getHttpUrl();
                            System.out.println(httpUrl);
                            complaintDto.setPhotoUrl(httpUrl);
                            complaintList.put(patientId, complaintDto);
                            System.out.println(complaintDto.getMessage());
                            System.out.println("sfbbajdfvbhnsjasjaja");
                            execute(botService.switchSpeciality(chatId.toString()));
                            patientService.updateState(chatId, PatientState.SWITCH_SPECIALITY);
                            patientStateHashMap.put(chatId, PatientState.SWITCH_SPECIALITY);


                            return;
                        }
                        execute(botService.sendPhoto(chatId.toString()));
                    }case SEND_COMPLAINT -> {

                    }
                }


            }
        }


     else if (update.hasCallbackQuery()) {
            Long chatId = update.getCallbackQuery().getMessage().getChatId();
            String data = update.getCallbackQuery().getData();
            Optional<Patient> currentPatient = patientService.findByChatId(chatId);
            Long patientId=currentPatient.get().getId();
                switch (patientStateHashMap.get(chatId)){
                    case SWITCH_SPECIALITY -> {
                        switch (data) {
                            case "LOR"->{
                                if (doctorRepository.existsDoctorBySpeciality(data)) {
                                    ComplaintDto complaintDto=complaintList.get(patientId);
                                    PatientDto patientDto=patientsList.get(chatId);
                                    complaintDto.setDoctorId(doctorChatId);
                                    doctorChatId = doctorRepository.getChatIdBySpeciality(data);
                                    execute(botService.sendComplaintToDoctor(chatId.toString(),patientDto,complaintDto,photoFileIds.get(chatId),data));
                                    patientService.updateState(chatId, PatientState.SEND_COMPLAINT);
                                    patientStateHashMap.put(chatId, PatientState.SEND_COMPLAINT);

                                }
                            }case "send"->{

                            }
                        }
                    }


                }

           }


        }

}


