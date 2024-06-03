package uz.amon.bot;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import uz.amon.domain.entity.Complaint;
import uz.amon.domain.entity.Doctor;
import uz.amon.domain.entity.Patient;
import uz.amon.domain.entity.Photo;
import uz.amon.domain.enums.ComplaintStatus;
import uz.amon.domain.enums.DoctorState;
import uz.amon.domain.enums.PatientLanguage;
import uz.amon.domain.enums.PatientState;
import uz.amon.repository.ComplaintRepository;
import uz.amon.repository.DoctorRepository;
import uz.amon.repository.PatientRepository;
import uz.amon.repository.PhotoRepository;
import uz.amon.service.DoctorService;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AmonBot extends TelegramLongPollingBot
{
    @Value("${telegram.bot.token}")
    public String token;

    @Value("${telegram.bot.username}")
    public String username;

    @Value("${photo.uploadDir}")
    public String imgLocation;

    private final DoctorService doctorService;
    private final DoctorRepository doctorRepo;
    private final PatientRepository patientRepo;
    private final PhotoRepository photoRepo;
    private final ComplaintRepository complaintRepo;

    @Override
    public String getBotToken()
    {
        return token;
    }

    @Override
    public String getBotUsername()
    {
        return username;
    }

    @SneakyThrows
    @Override
    public void onUpdateReceived(Update update)
    {

        if (update.hasMessage())
        {
            Long chatId = update.getMessage().getChatId();

            if (doctorService.isDoctor(chatId))
                doctorHandler(update);
            else
                patientHandler(update);
        } else if (update.hasCallbackQuery())
        {
            CallbackQuery callbackQuery = update.getCallbackQuery();
            Long chatId = callbackQuery.getMessage().getChatId();
            if (!doctorService.isDoctor(chatId)) // TODO for patient
            {
                Patient fromDb = patientRepo.findByChatId(chatId);
                switch (fromDb.getState())
                {
                    case PHONE:
                        patientLanguageHandler(update);
                        break;
                    case CHOOSE_DOCTOR:
                        patientSelectDoctor(update);
                        break;
                }
            } else if (doctorService.isDoctor(chatId)) // TODO for Doctor
            {
                Doctor fromDb = doctorRepo.findByChatId(chatId);

                switch (fromDb.getState())
                {
                    case START:
                        doctorAnswerHandler(update);
                }
            }
        }
    }


    private void patientSelectDoctor(Update update) throws TelegramApiException
    {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        Long chatId = callbackQuery.getMessage().getChatId();

        String data = callbackQuery.getData();
        Patient fromDb = patientRepo.findByChatId(chatId);

        Doctor doctor = doctorRepo.findById(Long.valueOf(data)).get();

        Complaint complaint = new Complaint();
        complaint.setDoctorId(doctor.getId());
        complaint.setWriterId(fromDb.getId());
        complaintRepo.save(complaint);
        fromDb.setComplaint(complaint);


        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);

        if (fromDb.getLanguage().equals(PatientLanguage.UZ))
            sendMessage.setText("Analiz rasmini yuboring");
        else if (fromDb.getLanguage().equals(PatientLanguage.RU))
            sendMessage.setText("Отправьте фото анализ");

        fromDb.setState(PatientState.PHOTO);
        patientRepo.save(fromDb);
        execute(sendMessage);

    }


    private void patientLanguageHandler(Update update) throws TelegramApiException
    {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        Long chatId = callbackQuery.getMessage().getChatId();


        String data = callbackQuery.getData();
        Patient fromDb = patientRepo.findByChatId(chatId);
//        fromDb.setState(PatientState.PHONE);
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        switch (data)
        {
            case "uz":
            {
                fromDb.setLanguage(PatientLanguage.UZ);
                patientRepo.save(fromDb);
                sendMessage.setText("Telefon raqamni yuborish tugmasini bosing");
                break;
            }
            case "ru":
            {
                fromDb.setLanguage(PatientLanguage.RU);
                patientRepo.save(fromDb);
                sendMessage.setText("Нажмите кнопку «Отправить номер телефона».");
                break;
            }
        }

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        List<KeyboardRow> rowList = new ArrayList<>();
        KeyboardButton button = new KeyboardButton();

        if (fromDb.getLanguage().equals(PatientLanguage.UZ))
            button.setText("Kontakt yuborish");
        else if (fromDb.getLanguage().equals(PatientLanguage.RU))
            button.setText("Отправить номер телефона");


        button.setRequestContact(true);

        KeyboardRow row = new KeyboardRow();
        row.add(button);
        rowList.add(row);
        markup.setKeyboard(rowList);
        sendMessage.setReplyMarkup(markup);

        execute(sendMessage);
    }

    private void patientHandler(Update update) throws TelegramApiException, IOException
    {
        Message message = update.getMessage();
        Long chatId = update.getMessage().getChatId();
        PatientState state = patientRepo.findPatientStateByChatId(chatId);

        if (message.hasText() && message.getText().equals("/start"))
        {
            state = PatientState.START;
        }

        switch (state)
        {
            case START:
                patientStartHandler(update);
                break;
            case PHONE:
                patientPhoneHandler(update);
                break;
            case FIRST_NAME:
                patientFirstnameHandler(update);
                break;
            case LAST_NAME:
                patientLastNameHandler(update);
                break;
            case PHOTO:
                patientPhotoHandler(update);
                break;
            case COMPLAINT_TEXT:
                patientComplaintTextHandler(update);
        }

    }

    private void patientComplaintTextHandler(Update update) throws TelegramApiException
    {
        Message message = update.getMessage();
        if (message.hasText())
        {
            Long chatId = message.getChatId();
            Patient patientFromDb = patientRepo.findByChatId(chatId);
            Complaint complaint = patientFromDb.getComplaint();
            complaint.setMessage(message.getText());
            complaint.setStatus(ComplaintStatus.CREATED_NOT_SENDED);
            complaintRepo.save(complaint);

            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);

            if (patientFromDb.getLanguage().equals(PatientLanguage.UZ))
                sendMessage.setText("Shikoyatingiz yuborildi, iltimos shifokor javobini kuting");
            else if (patientFromDb.getLanguage().equals(PatientLanguage.RU))
                sendMessage.setText("Ваша жалоба отправлена, дождитесь ответа врача");

            patientFromDb.setState(PatientState.WAIT_ANSWER_DOCTOR);
            patientRepo.save(patientFromDb);
            execute(sendMessage);


            Long doctorId = complaint.getDoctorId();

            SendPhoto sendPhoto = new SendPhoto();

            sendPhoto.setChatId(doctorRepo.findById(doctorId).get().getChatId());

            String photoPath = complaint.getPhoto().getSystemPath();
            sendPhoto.setPhoto(new InputFile(new java.io.File(photoPath)));
            sendPhoto.setCaption("From : " + patientFromDb.getFirstname() + " " + patientFromDb.getLastname() +
                    "\nMessage : " + complaint.getMessage());


            InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> inlineButtons = new ArrayList<>();
            List<InlineKeyboardButton> buttonList1 = new ArrayList<>();
            InlineKeyboardButton ok = new InlineKeyboardButton();
            InlineKeyboardButton cancel = new InlineKeyboardButton();
            ok.setText("Ответить");
            cancel.setText("Отклонить");

            ok.setCallbackData(complaint.getId() + "ok");
            cancel.setCallbackData(complaint.getId() + "cancel");

            buttonList1.add(ok);
            buttonList1.add(cancel);
            inlineButtons.add(buttonList1);
            inlineKeyboardMarkup.setKeyboard(inlineButtons);
            sendPhoto.setReplyMarkup(inlineKeyboardMarkup);

            execute(sendPhoto);

            complaint.setStatus(ComplaintStatus.SENDED_WAIT_DOCTOR_ANSWER);
            complaintRepo.save(complaint);
        }

    }

    private void patientPhotoHandler(Update update) throws TelegramApiException, IOException
    {
        Message message = update.getMessage();
        Long chatId = message.getChatId();
        if (message.hasPhoto())
        {
            String fileId = message.getPhoto().stream()
                    .max(Comparator.comparing(PhotoSize::getFileSize))
                    .orElseThrow(() -> new IllegalStateException("Photo is missing"))
                    .getFileId();
            GetFile getFileMethod = new GetFile(fileId);
            File file = execute(getFileMethod);
            String fileId1 = file.getFileId();
            String filePath = file.getFilePath();
            String fileName = UUID.randomUUID() + Paths.get(filePath).getFileName().toString();
            String fileUrl = "https://api.telegram.org/file/bot" + getBotToken() + "/" + filePath;
            URL url = new URL(fileUrl);
            InputStream input = url.openStream();
            String targetPath = imgLocation + fileName;
            Files.copy(input, Paths.get(targetPath), StandardCopyOption.REPLACE_EXISTING);
            input.close();


            Photo photo = new Photo(fileName, targetPath, "http://localhost:8080/complaint/image/bla-bla");
            Photo saved = photoRepo.save(photo);

            Patient patientFromDb = patientRepo.findByChatId(chatId);
            Complaint complaintOfPatient = patientFromDb.getComplaint();
            complaintOfPatient.setPhoto(saved);
            complaintOfPatient.setStatus(ComplaintStatus.CREATED_NOT_SENDED);
            complaintRepo.save(complaintOfPatient);

            patientFromDb.setState(PatientState.COMPLAINT_TEXT);
            patientRepo.save(patientFromDb);

            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            if (patientFromDb.getLanguage().equals(PatientLanguage.UZ))
                sendMessage.setText("Shifokorga shikoyatingizni batafsil yozib qoldring");
            else if (patientFromDb.getLanguage().equals(PatientLanguage.RU))
                sendMessage.setText("Подробно напишите жалобу на врачу.");

            execute(sendMessage);
        } else
        {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText("RASM yubor !!!");
            execute(sendMessage);
        }
    }


    private void patientLastNameHandler(Update update) throws TelegramApiException
    {
        Message message = update.getMessage();
        Long chatId = message.getChatId();

        if (message.hasText())
        {
            Patient fromDb = patientRepo.findByChatId(chatId);
            fromDb.setLastname(message.getText());
            patientRepo.save(fromDb);

            patientChooseDoctor(update);
        }

    }

    private void patientFirstnameHandler(Update update) throws TelegramApiException
    {
        Message message = update.getMessage();
        Long chatId = message.getChatId();
        Patient fromDb = patientRepo.findByChatId(chatId);

        if (message.hasText())
        {
            fromDb.setFirstname(message.getText());
            fromDb.setState(PatientState.LAST_NAME);
            patientRepo.save(fromDb);

            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);

            if (fromDb.getLanguage().equals(PatientLanguage.UZ))
                sendMessage.setText("Familyangizni kiriting");
            else if (fromDb.getLanguage().equals(PatientLanguage.RU))
                sendMessage.setText("Введите фамилию");

            execute(sendMessage);
        }

    }

    private void patientPhoneHandler(Update update) throws TelegramApiException
    {
        Message message = update.getMessage();
        Long chatId = message.getChatId();
        if (message.hasContact())
        {
            Contact contact = message.getContact();
            Patient fromDb = patientRepo.findByChatId(chatId);
            if (contact.getUserId().equals(message.getFrom().getId()))
            {
                String phone = contact.getPhoneNumber();
                fromDb.setPhone(phone);
                fromDb.setState(PatientState.FIRST_NAME);
                patientRepo.save(fromDb);
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(chatId);
                if (fromDb.getLanguage().equals(PatientLanguage.UZ))
                    sendMessage.setText("Ismingizni kiriting");
                else if (fromDb.getLanguage().equals(PatientLanguage.RU))
                    sendMessage.setText("Введите ваше имя");
                execute(sendMessage);
            } else
            {
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(chatId);

                if (fromDb.getLanguage().equals(PatientLanguage.UZ))
                    sendMessage.setText("Iltimos o'z telefon raqamingizni yuboring");
                else if (fromDb.getLanguage().equals(PatientLanguage.RU))
                    sendMessage.setText("Пожалуйста, пришлите свой номер телефона");

                execute(sendMessage);
            }
        }
    }

    private void patientStartHandler(Update update) throws TelegramApiException
    {
        SendMessage sendMessage = new SendMessage();
        Long chatId = update.getMessage().getChatId();
        sendMessage.setChatId(chatId);
        sendMessage.setText("Assalomu alaykum botga xush kelibsiz , Tilni tanlang  \n \n" +
                "Привет, добро пожаловать в бот, выбираем язык");

        Patient patient = new Patient();
        if (patientRepo.existsByChatId(chatId))
        {
            Patient existPatient = patientRepo.findByChatId(chatId);
            patient.setId(existPatient.getId());
        }
        patient.setChatId(chatId);
        patient.setState(PatientState.PHONE);
        patientRepo.save(patient);


        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> inlineButtons = new ArrayList<>();
        List<InlineKeyboardButton> buttonList1 = new ArrayList<>();
        InlineKeyboardButton uz = new InlineKeyboardButton();
        InlineKeyboardButton ru = new InlineKeyboardButton();
        uz.setText("Uz \uD83C\uDDFA\uD83C\uDDFF");
        ru.setText("Ru \uD83C\uDDF7\uD83C\uDDFA");

        uz.setCallbackData("uz");
        ru.setCallbackData("ru");

        buttonList1.add(uz);
        buttonList1.add(ru);
        inlineButtons.add(buttonList1);
        inlineKeyboardMarkup.setKeyboard(inlineButtons);
        sendMessage.setReplyMarkup(inlineKeyboardMarkup);
        execute(sendMessage);
    }

    private void patientChooseDoctor(Update update) throws TelegramApiException
    {
        Message message = update.getMessage();
        Long chatId = message.getChatId();
        Patient fromDb = patientRepo.findByChatId(chatId);


        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);

        if (fromDb.getLanguage().equals(PatientLanguage.UZ))
            sendMessage.setText("Pastdan kerakli shifokorni tanlang");
        else if (fromDb.getLanguage().equals(PatientLanguage.RU))
            sendMessage.setText("Выберите ниже нужного вам врача");

        List<Doctor> doctorList = doctorService.getList();

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> inlineButtons = new ArrayList<>();

        for (Doctor doctor : doctorList)
        {
            List<InlineKeyboardButton> row = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(doctor.getFirstname() + " " + doctor.getLastname());
            button.setCallbackData(doctor.getId().toString());
            row.add(button);
            inlineButtons.add(row);
        }
/*
        List<InlineKeyboardButton> buttonList1 = new ArrayList<>();
        List<InlineKeyboardButton> buttonList2 = new ArrayList<>();
        List<InlineKeyboardButton> buttonList3 = new ArrayList<>();
        List<InlineKeyboardButton> buttonList4 = new ArrayList<>();
        List<InlineKeyboardButton> buttonList5 = new ArrayList<>();
        List<InlineKeyboardButton> buttonList6 = new ArrayList<>();

        InlineKeyboardButton d1 = new InlineKeyboardButton();
        InlineKeyboardButton d2 = new InlineKeyboardButton();
        InlineKeyboardButton d3 = new InlineKeyboardButton();
        InlineKeyboardButton d4 = new InlineKeyboardButton();
        InlineKeyboardButton d5 = new InlineKeyboardButton();
        InlineKeyboardButton d6 = new InlineKeyboardButton();

        d1.setText(doctorList.get(0).getFirstname() + " " + doctorList.get(0).getLastname());
        d2.setText(doctorList.get(1).getFirstname() + " " + doctorList.get(2).getLastname());
        d3.setText(doctorList.get(2).getFirstname() + " " + doctorList.get(3).getLastname());
        d4.setText(doctorList.get(3).getFirstname() + " " + doctorList.get(4).getLastname());
        d5.setText(doctorList.get(4).getFirstname() + " " + doctorList.get(6).getLastname());
        d6.setText(doctorList.get(5).getFirstname() + " " + doctorList.get(5).getLastname());

        d1.setCallbackData(doctorList.get(0).getId().toString());
        d2.setCallbackData(doctorList.get(1).getId().toString());
        d3.setCallbackData(doctorList.get(2).getId().toString());
        d4.setCallbackData(doctorList.get(3).getId().toString());
        d5.setCallbackData(doctorList.get(4).getId().toString());
        d6.setCallbackData(doctorList.get(5).getId().toString());

        buttonList1.add(d1);
        buttonList2.add(d2);
        buttonList3.add(d3);
        buttonList4.add(d4);
        buttonList5.add(d5);
        buttonList6.add(d6);

        inlineButtons.add(buttonList1);
        inlineButtons.add(buttonList2);
        inlineButtons.add(buttonList3);
        inlineButtons.add(buttonList4);
        inlineButtons.add(buttonList5);
        inlineButtons.add(buttonList6);
*/
        inlineKeyboardMarkup.setKeyboard(inlineButtons);
        sendMessage.setReplyMarkup(inlineKeyboardMarkup);

        execute(sendMessage);

        fromDb.setState(PatientState.CHOOSE_DOCTOR);
        patientRepo.save(fromDb);
    }

    //TODO ==========================================================================================
    private void doctorHandler(Update update) throws TelegramApiException
    {
        Message message = update.getMessage();
        Long doctorChatId = message.getChatId();
        Doctor fromDb = doctorRepo.findByChatId(doctorChatId);

        if (message.hasText() && message.getText().equals("/start"))
        {
            fromDb.setState(DoctorState.START);
            doctorRepo.save(fromDb);
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(doctorChatId);
            sendMessage.setText("Welcome doctor page");
            execute(sendMessage);
            return;
        }

        switch (fromDb.getState())
        {
            case COMPLAINT_ANSWER:
            {
                String answerOfDoctor = message.getText();
                Long currentComplaintId = fromDb.getCurrentComplaintId();
                Complaint complaint = complaintRepo.findById(currentComplaintId).get();
                complaint.setAnswerOfDoctor(answerOfDoctor);
                complaint.setStatus(ComplaintStatus.ANSWERED);
                complaintRepo.save(complaint);

                SendMessage patientMessage = new SendMessage();
                Patient patient = patientRepo.findById(complaint.getWriterId()).get();
                patientMessage.setChatId( patient.getChatId() );
                patientMessage.setText("Ответ врача:\n"+answerOfDoctor);
                execute(patientMessage);

                SendMessage doctorMessage = new SendMessage();
                doctorMessage.setChatId(doctorChatId);
                doctorMessage.setText("Спасибо за ответ, ваш ответ доставлен");
                execute(doctorMessage);
                break;
            }

        }

    }


    private void doctorAnswerHandler(Update update) throws TelegramApiException
    {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        Long doctorChatId = callbackQuery.getMessage().getChatId();

        String data = callbackQuery.getData();
        if (data.contains("ok"))
        {
            Long complaintId = Long.valueOf(data.replaceAll("ok", ""));
            Complaint complaint = complaintRepo.findById(complaintId).get();

            complaint.setStatus(ComplaintStatus.DOCTOR_ACCEPTED_NOT_ANSWERED);
            complaintRepo.save(complaint);

            Doctor fromDb = doctorRepo.findByChatId(doctorChatId);
            fromDb.setState(DoctorState.COMPLAINT_ANSWER);
            fromDb.setCurrentComplaintId(complaintId);
            doctorRepo.save(fromDb);

            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(doctorChatId);
            sendMessage.setText("Запишите свой ответ для пациента");
            execute(sendMessage);
        } else if (data.contains("cancel"))
        {
            Long complaintId = Long.valueOf(data.replaceAll("cancel", ""));
            Complaint complaint = complaintRepo.findById(complaintId).get();
            complaint.setStatus(ComplaintStatus.REJECTED_BY_DOCTOR);
            complaintRepo.save(complaint);
            SendMessage sendMessage = new SendMessage();

            Patient patient = patientRepo.findById(complaint.getWriterId()).get();
            sendMessage.setChatId(patient.getChatId());
            sendMessage.setText(complaint.getMessage() + "\n -Извини , Врач не может ответить на ваш вопрос ");
            execute(sendMessage);
        }

    }
}
