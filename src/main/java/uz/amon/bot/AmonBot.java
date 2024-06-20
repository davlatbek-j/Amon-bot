package uz.amon.bot;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ForwardMessage;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.*;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import uz.amon.domain.entity.*;
import uz.amon.domain.enums.ComplaintStatus;
import uz.amon.domain.enums.DoctorState;
import uz.amon.domain.enums.PatientLanguage;
import uz.amon.domain.enums.PatientState;
import uz.amon.repository.*;
import uz.amon.service.DoctorService;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.*;

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
    private final PatientSessionNumberRepo patientSessionNumberRepo;

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
                doctorMessageHandler(update);
            else
            {
                patientMessageHandler(update);
            }
        } else if (update.hasCallbackQuery())
        {
            CallbackQuery callbackQuery = update.getCallbackQuery();
            Long chatId = callbackQuery.getMessage().getChatId();
            if (doctorService.isDoctor(chatId))
                doctorCallBackQueryHandler(update);
            else
                patientCallBackHandler(update);
        }
    }

    //TODO ========================================================================
    //TODO ========================================================================
    //TODO ========================================================================


    private void patientMessageHandler(Update update) throws TelegramApiException, IOException
    {
        Message message = update.getMessage();
        Long chatId = update.getMessage().getChatId();

        PatientState state = patientRepo.findPatientStateByChatId(chatId);

        if (message.hasText() && message.getText().equals("/start"))
        {
/*            if (patientRepo.existsByChatId(chatId))
            {
                Patient patient = patientRepo.findByChatId(chatId);
                if (patient.getState()!=null && patient.getState()==PatientState.WAIT_ANSWER_OF_COMPLAINT)
                {
                    SendMessage sendMessage = new SendMessage();
                    sendMessage.setChatId(chatId);
                    PatientLanguage lang = patient.getLanguage();
                    if (lang==PatientLanguage.UZ)
                        sendMessage.setText("Uzur, shifokordan javob olmagunizcha botdan foydalana olmaysiz!");
                    else if (lang==PatientLanguage.RU)
                        sendMessage.setText("К сожалению, вы не сможете использовать бота, пока не получите ответ от врача!");
                    execute(sendMessage);
                    return;
                }
            }*/
            state = PatientState.START;
        }

        if (message.hasText() && (message.getText().equals("⬅️ Назад") || message.getText().equals("⬅️ Ortga qaytish")))
        {
            patientBackButtonHandler(update);
            return;
        }

        switch (state)
        {
            case START:
                patientStartHandler(update);
                break;
            case PHONE:
                patientPhoneHandler(update);
                break;
            case FULL_NAME:
                patientFullnameHandler(update);
                break;
            case PHOTO:
                patientComplaintPhotoHandler(update);
                break;
            case COMPLAINT_TEXT:
                patientComplaintConfirmRequest(update);
                break;
            case WRITE_REPLY_MESSAGE:
                patientReplyMessageHandler(update);
                break;
        }

    }

    private void patientCallBackHandler(Update update) throws TelegramApiException
    {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        Long chatId = callbackQuery.getMessage().getChatId();

        Patient fromDb = patientRepo.findByChatId(chatId);

        String data = callbackQuery.getData();

        if (data.equals("uz") || data.equals("ru"))
            patientLanguageCallBackHandler(update);
        else if (data.contains("doctor-id"))
            patientChooseDoctorCallBackHandler(update);
        else if (data.contains("reply-message-to-doctor"))
            patientReplyMessageRequest(update);
        else if (data.equals("patient-re-choose-doctor"))
            patientReChooseMessageRequest(update);
        else if (data.contains("confirm-complaint") || data.contains("edit-complaint"))
            patientComplaintConfirmCallBackHandler(update);

    }

    private void patientBackButtonHandler(Update update) throws TelegramApiException
    {
        Message message = update.getMessage();
        Long chatId = message.getChatId();
        Patient fromDb = patientRepo.findByChatId(chatId);
        String data = message.getText();

        PatientState state = fromDb.getState();

        switch (state)
        {
            case PHONE:
                patientStartHandler(update);
                break;
            case FULL_NAME:
                patientPhoneRequest(chatId);
                break;
            case CHOOSE_DOCTOR:
                patientFullNameRequest(chatId);
                break;
            case PHOTO:
                patientChooseDoctorRequest(update);
                break;
            case COMPLAINT_TEXT:
                patientComplaintPhotoRequest(chatId);
                break;
        }

    }

    // TODO ------------------------------------------------------------------------

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
            patient.setId(patientRepo.findByChatId(chatId).getId());
        }
        patient.setChatId(chatId);
        patient.setState(PatientState.CHOOSE_LANGUAGE);
        patientRepo.save(patient);


        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> inlineButtons = new ArrayList<>();
        List<InlineKeyboardButton> buttonList1 = new ArrayList<>();
        InlineKeyboardButton uz = new InlineKeyboardButton();
        InlineKeyboardButton ru = new InlineKeyboardButton();
        uz.setText("Uzb \uD83C\uDDFA\uD83C\uDDFF");
        ru.setText("Rus \uD83C\uDDF7\uD83C\uDDFA");

        uz.setCallbackData("uz");
        ru.setCallbackData("ru");

        buttonList1.add(uz);
        buttonList1.add(ru);
        inlineButtons.add(buttonList1);
        inlineKeyboardMarkup.setKeyboard(inlineButtons);
        sendMessage.setReplyMarkup(inlineKeyboardMarkup);
        execute(sendMessage);
    }

    private void patientLanguageCallBackHandler(Update update) throws TelegramApiException
    {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        Long chatId = callbackQuery.getMessage().getChatId();


        String data = callbackQuery.getData();
        Patient patient = patientRepo.findByChatId(chatId);

        switch (data)
        {
            case "uz":
            {
                patient.setLanguage(PatientLanguage.UZ);
                patientRepo.save(patient);
                break;
            }
            case "ru":
            {
                patient.setLanguage(PatientLanguage.RU);
                patientRepo.save(patient);
                break;
            }
        }
        patientPhoneRequest(chatId);
    }

    private void patientPhoneRequest(Long chatId) throws TelegramApiException
    {
        Patient patient = patientRepo.findByChatId(chatId);

        patient.setState(PatientState.PHONE);

        patientRepo.save(patient);

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        if (patient.getLanguage() == PatientLanguage.UZ)
            sendMessage.setText("Siz bilan bog'lana olishimiz uchun «Telefon raqamni yuborish» tugmasini bosing");
        else if (patient.getLanguage() == PatientLanguage.RU)
            sendMessage.setText("Нажмите «Отправить номер телефона», чтобы мы могли связаться с вами.");


        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        List<KeyboardRow> rowList = new ArrayList<>();
        KeyboardButton button1 = new KeyboardButton();
        KeyboardButton button2 = new KeyboardButton();

        markup.setResizeKeyboard(true);
        if (patient.getLanguage().equals(PatientLanguage.UZ))
        {
            button1.setText("Telefon raqamni yuborish");
            button2.setText("⬅️ Ortga qaytish");
        } else if (patient.getLanguage().equals(PatientLanguage.RU))
        {
            button1.setText("Отправить номер телефона");
            button2.setText("⬅️ Назад");
        }

        button1.setRequestContact(true);

        KeyboardRow row = new KeyboardRow();
        row.add(button1);

        KeyboardRow row2 = new KeyboardRow();
        row2.add(button2);

        rowList.add(row);
        rowList.add(row2);

        markup.setKeyboard(rowList);
        markup.setResizeKeyboard(true);
        sendMessage.setReplyMarkup(markup);

        execute(sendMessage);
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
                patientRepo.save(fromDb);

                patientFullNameRequest(chatId);

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

    private void patientFullNameRequest(Long chatId) throws TelegramApiException
    {
        Patient patient = patientRepo.findByChatId(chatId);
        patient.setState(PatientState.FULL_NAME);
        patientRepo.save(patient);

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        if (patient.getLanguage().equals(PatientLanguage.UZ))
            sendMessage.setText("Ism-familyangizni kiriting");
        else if (patient.getLanguage().equals(PatientLanguage.RU))
            sendMessage.setText("Введите ваше имя и фамилию");
        execute(sendMessage);
    }

    private void patientFullnameHandler(Update update) throws TelegramApiException
    {
        Message message = update.getMessage();
        Long chatId = message.getChatId();
        Patient fromDb = patientRepo.findByChatId(chatId);

        if (message.hasText())
        {
            fromDb.setFullName(message.getText());
            fromDb.setState(PatientState.PHOTO);
            patientRepo.save(fromDb);

            patientChooseDoctorRequest(update);
        }

    }


    private void patientChooseDoctorRequest(Update update) throws TelegramApiException
    {
        Message message = update.getMessage();
        Long chatId = message.getChatId();
        Patient fromDb = patientRepo.findByChatId(chatId);

        fromDb.setState(PatientState.CHOOSE_DOCTOR);
        patientRepo.save(fromDb);

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

            if (fromDb.getLanguage().equals(PatientLanguage.UZ))
                button.setText(doctor.getSpecialityUz());
            else if (fromDb.getLanguage().equals(PatientLanguage.RU))
                button.setText(doctor.getSpecialityRu());

            button.setCallbackData("doctor-id" + doctor.getId().toString());
            row.add(button);
            inlineButtons.add(row);
        }

        inlineKeyboardMarkup.setKeyboard(inlineButtons);
        sendMessage.setReplyMarkup(inlineKeyboardMarkup);

        execute(sendMessage);
    }

    private void patientChooseDoctorCallBackHandler(Update update) throws TelegramApiException
    {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        Long chatId = callbackQuery.getMessage().getChatId();

        String data = callbackQuery.getData().replaceAll("doctor-id", "");
        Patient patient = patientRepo.findByChatId(chatId);


        Doctor doctor = doctorRepo.findById(Long.valueOf(data)).get();


        Complaint complaint = new Complaint();
        complaint.setDoctorId(doctor.getId());
        complaint.setWriterId(patient.getId());

        complaintRepo.save(complaint);
        patient.setComplaint(complaint);

        patientRepo.save(patient);

        patientComplaintPhotoRequest(chatId);

    }

    private void patientComplaintPhotoRequest(Long chatId) throws TelegramApiException
    {
        Patient patient = patientRepo.findByChatId(chatId);

        Doctor doctor = doctorRepo.findById(patient.getComplaint().getDoctorId()).get();

        patient.setState(PatientState.PHOTO);
        patientRepo.save(patient);

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);

        if (patient.getLanguage().equals(PatientLanguage.UZ))
            sendMessage.setText(doctor.getSpecialityUz() + "ga Analiz rasmini yuboring");
        else if (patient.getLanguage().equals(PatientLanguage.RU))
            sendMessage.setText("Отправить фото анализа " + doctor.getSpecialityRu() + "у");

        execute(sendMessage);
    }

    private void patientComplaintPhotoHandler(Update update) throws TelegramApiException, IOException
    {
        Message message = update.getMessage();
        Long chatId = message.getChatId();
        Patient patientFromDb = patientRepo.findByChatId(chatId);

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


            Photo photo = new Photo(fileName, targetPath);
            Photo saved = photoRepo.save(photo);

            Complaint complaintOfPatient = patientFromDb.getComplaint();
            complaintOfPatient.setPhoto(saved);
            complaintOfPatient.setStatus(ComplaintStatus.CREATED_NOT_SENDED);
            complaintRepo.save(complaintOfPatient);

            patientComplaintTextRequest(chatId);

        } else
        {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            if (patientFromDb.getLanguage().equals(PatientLanguage.RU))
                sendMessage.setText("Пожалуйста, пришлите фото анализа!");
            else if (patientFromDb.getLanguage().equals(PatientLanguage.UZ))
                sendMessage.setText("Iltimos analiz rasmini yuboring!");
            execute(sendMessage);
        }
    }

    private void patientComplaintTextRequest(Long chatId) throws TelegramApiException
    {
        Patient patient = patientRepo.findByChatId(chatId);
        patient.setState(PatientState.COMPLAINT_TEXT);
        patientRepo.save(patient);

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        if (patient.getLanguage().equals(PatientLanguage.UZ))
            sendMessage.setText("Shifokorga shikoyatingizni batafsil yozib qoldring");
        else if (patient.getLanguage().equals(PatientLanguage.RU))
            sendMessage.setText("Подробно напишите жалобу на врачу.");

        execute(sendMessage);
    }

    //Please do not delete even if it is not used
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

            Long doctorId = complaint.getDoctorId();
            Doctor doctor = doctorRepo.findById(doctorId).get();


            //Bu klient bu doktorga oldin ham yozganmi yoki yo'qmi ya'ni bu klient bu doktor uchun yangi klientmi?
            boolean existsByDoctorIdAndWriterId = patientSessionNumberRepo.existsByDoctorIdAndPatientId(doctorId, patientFromDb.getId());

            if (existsByDoctorIdAndWriterId)
            {
                //agar bu klient bu doktorga oldin ham yozgan bo'lsa
                PatientSessionAndThemeNumber sessionAndThemeNumber = patientSessionNumberRepo.findByPatientIdAndDoctorId(patientFromDb.getId(), doctorId);
                complaint.setSessionNum(sessionAndThemeNumber.getSessionNumber());

                //bu klient bu doktor bilan oxirgi marta n-raqamli temada gaplashgan....

                sessionAndThemeNumber.setLastThemeNumber(sessionAndThemeNumber.getLastThemeNumber() + 1);
                complaint.setThemeNum(sessionAndThemeNumber.getLastThemeNumber());


                patientSessionNumberRepo.save(sessionAndThemeNumber);
            } else // agar bu klient bu doktorga 1-marta yozayotgan bo'lsa
            {
                PatientSessionAndThemeNumber sessionNumber = new PatientSessionAndThemeNumber();
                sessionNumber.setDoctorId(doctorId);
                sessionNumber.setPatientId(patientFromDb.getId());
                //doktorda shu vaqtgacha n ta sessiya ochilgan bo'lsa endi buni n+1 sessiay boladi
                sessionNumber.setSessionNumber(doctor.getLastSessionNumber() == null ? 1 : doctor.getLastSessionNumber() + 1);

                //Agar bu klient bu doktorga 1-marta yozayotgan bo'lsa bo'lsa tema nomeri 1 boladi
                sessionNumber.setLastThemeNumber(1);

                doctor.setLastSessionNumber(sessionNumber.getSessionNumber());
                doctorRepo.save(doctor);

                patientSessionNumberRepo.save(sessionNumber);

                complaint.setSessionNum(sessionNumber.getSessionNumber());
                complaint.setThemeNum(sessionNumber.getLastThemeNumber());
            }
            complaintRepo.save(complaint);


            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);

            if (patientFromDb.getLanguage().equals(PatientLanguage.UZ))
                sendMessage.setText("Shikoyatingiz yuborildi, iltimos shifokor javobini kuting");
            else if (patientFromDb.getLanguage().equals(PatientLanguage.RU))
                sendMessage.setText("Ваша жалоба отправлена, дождитесь ответа врача");

//            sendMessage.setReplyMarkup(null);
            patientFromDb.setState(PatientState.WAIT_ANSWER_OF_REPLY_MESSAGE);
            patientRepo.save(patientFromDb);
            execute(sendMessage);


            SendPhoto sendPhoto = new SendPhoto();

            sendPhoto.setChatId(doctorRepo.findById(doctorId).get().getChatId());

            Date lastWritedDate = patientFromDb.getLastMessageDate();

            String lastTime = "Новый, первый раз"; //noviy perviy raz
            if (lastWritedDate != null)
                lastTime = new SimpleDateFormat("yyyy-MM-dd  HH:mm").format(lastWritedDate);

            String photoPath = complaint.getPhoto().getSystemPath();
            sendPhoto.setPhoto(new InputFile(new java.io.File(photoPath)));

            sendPhoto.setCaption("От : " + patientFromDb.getFullName() +
                    "\nТелефон пациента : +" + patientFromDb.getPhone() +
                    "\nСообщение : " + complaint.getMessage() +
                    "\n\n\n____________\nПоследние сообщения : " + lastTime +          //Poslednie sobsheniya
                    "\nСессия: #c" + complaint.getSessionNum() +                          //Sessiya
                    "\nТема: #т" + complaint.getThemeNum() +
                    "\nЧат: #c" + complaint.getSessionNum() + "т" + complaint.getThemeNum()
            );


            InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> inlineButtons = new ArrayList<>();
            List<InlineKeyboardButton> buttonList1 = new ArrayList<>();
            InlineKeyboardButton ok = new InlineKeyboardButton();
            InlineKeyboardButton cancel = new InlineKeyboardButton();
            ok.setText("Ответить");
            cancel.setText("Отклонить");

            ok.setCallbackData("ok" + complaint.getId());
            cancel.setCallbackData("cancel" + complaint.getId());


            buttonList1.add(ok);
            buttonList1.add(cancel);
            inlineButtons.add(buttonList1);
            inlineKeyboardMarkup.setKeyboard(inlineButtons);
            sendPhoto.setReplyMarkup(inlineKeyboardMarkup);

            execute(sendPhoto);

            complaint.setStatus(ComplaintStatus.SENDED_WAIT_DOCTOR_ANSWER);
            complaintRepo.save(complaint);

            patientFromDb.setLastMessageDate(new Date());
            patientRepo.save(patientFromDb);
        }

    }

    private void patientComplaintConfirmRequest(Update update) throws TelegramApiException
    {
        Message message = update.getMessage();
        if (message.hasText())
        {
            Long chatId = message.getChatId();
            Patient patient = patientRepo.findByChatId(chatId);
            Complaint complaint = patient.getComplaint();
            complaint.setMessage(message.getText());
            complaint.setStatus(ComplaintStatus.NOT_CONFIRMED);

            complaintRepo.save(complaint);

            patient.setState(PatientState.CONFIRM_COMPLAINT);
            patient.setConfirmComplaint(false);
            patientRepo.save(patient);

            PatientLanguage lang = patient.getLanguage();
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            if (lang == PatientLanguage.UZ)
                sendMessage.setText("Murojaatingiz shifokorga quyidagi ko'rinishda yetkaziladi , Iltimos murojatingizni tasdiqlang :");
            else if (lang == PatientLanguage.RU)
                sendMessage.setText("Ваш запрос будет доставлен врачу в следующей форме. Пожалуйста, подтвердите свой запрос:");

            execute(sendMessage);


            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setChatId(chatId);
            String photoPath = complaint.getPhoto().getSystemPath();
            sendPhoto.setPhoto(new InputFile(new java.io.File(photoPath)));
            sendPhoto.setCaption(complaint.getMessage());

            InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> inlineButtons = new ArrayList<>();
            List<InlineKeyboardButton> buttonList1 = new ArrayList<>();
            InlineKeyboardButton confirm = new InlineKeyboardButton();
            InlineKeyboardButton edit = new InlineKeyboardButton();


            if (lang == PatientLanguage.UZ)
            {
                confirm.setText("Tasdiqlash✅");
                edit.setText("O'zgartirish✏️");
            } else if (lang == PatientLanguage.RU)
            {
                confirm.setText("Подтверждать✅");
                edit.setText("Редактировать✏️");
            }

            confirm.setCallbackData("confirm-complaint" + complaint.getId());
            edit.setCallbackData("edit-complaint" + complaint.getId());


            buttonList1.add(confirm);
            buttonList1.add(edit);
            inlineButtons.add(buttonList1);
            inlineKeyboardMarkup.setKeyboard(inlineButtons);
            sendPhoto.setReplyMarkup(inlineKeyboardMarkup);

            execute(sendPhoto);
        }

    }

    private void patientComplaintConfirmCallBackHandler(Update update) throws TelegramApiException
    {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        Long chatId = callbackQuery.getMessage().getChatId();
        String data = callbackQuery.getData();

        Patient patient = patientRepo.findByChatId(chatId);

        if (data.contains("confirm-complaint"))
        {
            long complaintId = Long.parseLong(data.replace("confirm-complaint", ""));
            Complaint complaint = complaintRepo.findById(complaintId).get();

            if (patient.isConfirmComplaint())
                return;

            patient.setConfirmComplaint(true);
            patientRepo.save(patient);

            complaint.setStatus(ComplaintStatus.CONFIRMED);
            complaintRepo.save(complaint);

            Long doctorId = complaint.getDoctorId();
            Doctor doctor = doctorRepo.findById(doctorId).get();


            //Bu klient bu doktorga oldin ham yozganmi yoki yo'qmi ya'ni bu klient bu doktor uchun yangi klientmi?
            boolean existsByDoctorIdAndWriterId = patientSessionNumberRepo.existsByDoctorIdAndPatientId(doctorId, patient.getId());

            if (existsByDoctorIdAndWriterId)
            {
                //agar bu klient bu doktorga oldin ham yozgan bo'lsa
                PatientSessionAndThemeNumber sessionAndThemeNumber = patientSessionNumberRepo.findByPatientIdAndDoctorId(patient.getId(), doctorId);
                complaint.setSessionNum(sessionAndThemeNumber.getSessionNumber());

                //bu klient bu doktor bilan oxirgi marta n-raqamli temada gaplashgan....

                sessionAndThemeNumber.setLastThemeNumber(sessionAndThemeNumber.getLastThemeNumber() + 1);
                complaint.setThemeNum(sessionAndThemeNumber.getLastThemeNumber());


                patientSessionNumberRepo.save(sessionAndThemeNumber);
            } else // agar bu klient bu doktorga 1-marta yozayotgan bo'lsa
            {
                PatientSessionAndThemeNumber sessionNumber = new PatientSessionAndThemeNumber();
                sessionNumber.setDoctorId(doctorId);
                sessionNumber.setPatientId(patient.getId());
                //doktorda shu vaqtgacha n ta sessiya ochilgan bo'lsa endi buni n+1 sessiay boladi
                sessionNumber.setSessionNumber(doctor.getLastSessionNumber() == null ? 1 : doctor.getLastSessionNumber() + 1);

                //Agar bu klient bu doktorga 1-marta yozayotgan bo'lsa bo'lsa tema nomeri 1 boladi
                sessionNumber.setLastThemeNumber(1);

                doctor.setLastSessionNumber(sessionNumber.getSessionNumber());
                doctorRepo.save(doctor);

                patientSessionNumberRepo.save(sessionNumber);

                complaint.setSessionNum(sessionNumber.getSessionNumber());
                complaint.setThemeNum(sessionNumber.getLastThemeNumber());
            }
            complaintRepo.save(complaint);


            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);

            if (patient.getLanguage().equals(PatientLanguage.UZ))
                sendMessage.setText("Shikoyatingiz yuborildi, iltimos shifokor javobini kuting");
            else if (patient.getLanguage().equals(PatientLanguage.RU))
                sendMessage.setText("Ваша жалоба отправлена, дождитесь ответа врача");

            patient.setState(PatientState.WAIT_ANSWER_OF_COMPLAINT);
            patientRepo.save(patient);
            execute(sendMessage);


            SendPhoto sendPhoto = new SendPhoto();

            sendPhoto.setChatId(doctorRepo.findById(doctorId).get().getChatId());

            Date lastWritedDate = patient.getLastMessageDate();

            String lastTime = "Новый, первый раз"; //noviy perviy raz
            if (lastWritedDate != null)
                lastTime = new SimpleDateFormat("yyyy-MM-dd  HH:mm").format(lastWritedDate);

            String photoPath = complaint.getPhoto().getSystemPath();
            sendPhoto.setPhoto(new InputFile(new java.io.File(photoPath)));

            sendPhoto.setCaption("От : " + patient.getFullName() +
                    "\nТелефон пациента : +" + patient.getPhone() +
                    "\nСообщение : " + complaint.getMessage() +
                    "\n\n\n____________\nПоследние сообщения : " + lastTime +          //Poslednie sobsheniya
                    "\nСессия: #c" + complaint.getSessionNum() +                          //Sessiya
                    "\nТема: #т" + complaint.getThemeNum() +
                    "\nЧат: #c" + complaint.getSessionNum() + "т" + complaint.getThemeNum()
            );


            InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> inlineButtons = new ArrayList<>();
            List<InlineKeyboardButton> buttonList1 = new ArrayList<>();
            InlineKeyboardButton ok = new InlineKeyboardButton();
            InlineKeyboardButton cancel = new InlineKeyboardButton();
            ok.setText("Ответить");
            cancel.setText("Отклонить");

            ok.setCallbackData("ok" + complaint.getId());
            cancel.setCallbackData("cancel" + complaint.getId());


            buttonList1.add(ok);
            buttonList1.add(cancel);
            inlineButtons.add(buttonList1);
            inlineKeyboardMarkup.setKeyboard(inlineButtons);
            sendPhoto.setReplyMarkup(inlineKeyboardMarkup);

            execute(sendPhoto);


            patient.setLastMessageDate(new Date());
            patientRepo.save(patient);

        }
        else
        {
            if (patient.isConfirmComplaint())
                return;
            patient.setConfirmComplaint(true);
            patientRepo.save(patient);

            patientComplaintPhotoRequest(chatId);
        }

    }

    private void patientReplyMessageRequest(Update update) throws TelegramApiException
    {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        Long patientChatId = callbackQuery.getMessage().getChatId();
        Patient patient = patientRepo.findByChatId(patientChatId);

        String data = callbackQuery.getData();

        long doctorChatId = Long.parseLong(data.replaceAll("reply-message-to-doctor", ""));
        Doctor doctor = doctorRepo.findByChatId(doctorChatId);

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(patientChatId);
        if (patient.getLanguage().equals(PatientLanguage.RU))
            sendMessage.setText("Отправьте сообщение любого типа (текст, файл, фото, голос)");
        else if (patient.getLanguage().equals(PatientLanguage.UZ))
            sendMessage.setText("Har qanday turdagi xabar qoldiringingiz mumkin (matn, fayl, fotosurat, ovoz)");

        patient.setCurrentReplyDoctorChatId(doctorChatId);
        patient.setState(PatientState.WRITE_REPLY_MESSAGE);
        patientRepo.save(patient);

        execute(sendMessage);
    }

    private void patientReplyMessageHandler(Update update) throws TelegramApiException
    {
        Message message = update.getMessage();
        Long patientChatId = update.getMessage().getChatId();
        Patient patientFromDb = patientRepo.findByChatId(patientChatId);

        Long doctorChatId = patientFromDb.getCurrentReplyDoctorChatId();


        SendMessage doctorSendMessage = new SendMessage();
        doctorSendMessage.setChatId(doctorChatId);
        doctorSendMessage.setText("Новое сообщение от пациента :" + patientFromDb.getFullName() +
                "\nНомер телефона: +" + patientFromDb.getPhone());

        execute(doctorSendMessage);


        Date lastWritedDate = patientFromDb.getLastMessageDate();
        String lastTime = "Новый, первый раз"; //noviy perviy raz
        if (lastWritedDate != null)
            lastTime = new SimpleDateFormat("yyyy-MM-dd    HH:mm").format(lastWritedDate);


        Complaint complaint = patientFromDb.getComplaint();

        if (message.hasText())
        {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(doctorChatId);
            sendMessage.setText("\nСообщение: " + message.getText() +
                    "\n\n\n____________\nПоследние сообщения : " + lastTime +
                    "\nСессия: #c" + complaint.getSessionNum() +                          //Sessiya
                    "\nТема: #т" + complaint.getThemeNum() +
                    "\nЧат: #c" + complaint.getSessionNum() + "т" + complaint.getThemeNum()
            );

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
            List<InlineKeyboardButton> row = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText("Написать ответ");
            button.setCallbackData("reply-to-patient" + patientChatId);
            row.add(button);
            keyboard.add(row);
            markup.setKeyboard(keyboard);
            sendMessage.setReplyMarkup(markup);

            execute(sendMessage);

        } else if (message.hasPhoto())
        {
            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setChatId(String.valueOf(doctorChatId));
            sendPhoto.setPhoto(new InputFile(message.getPhoto().get(0).getFileId()));
            if (message.getCaption() != null)
            {
                sendPhoto.setCaption(message.getCaption() +
                        "\n\n\n____________\nПоследние сообщения : " + lastTime +
                        "\nСессия: #c" + complaint.getSessionNum() +
                        "\nТема: #т" + complaint.getThemeNum() +
                        "\nЧат: #c" + complaint.getSessionNum() + "т" + complaint.getThemeNum()
                );
            } else
                sendPhoto.setCaption("____________\nПоследние сообщения : " + lastTime +
                        "\nСессия: #c" + complaint.getSessionNum() +                          //Sessiya
                        "\nТема: #т" + complaint.getThemeNum() +
                        "\nЧат: #c" + complaint.getSessionNum() + "т" + complaint.getThemeNum());

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
            List<InlineKeyboardButton> row = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText("Написать ответ");
            button.setCallbackData("reply-to-patient" + patientChatId);
            row.add(button);
            keyboard.add(row);
            markup.setKeyboard(keyboard);
            sendPhoto.setReplyMarkup(markup);

            execute(sendPhoto);
        } else if (message.hasDocument())
        {
            SendDocument sendDocument = new SendDocument();
            sendDocument.setChatId(String.valueOf(doctorChatId));
            sendDocument.setDocument(new InputFile(message.getDocument().getFileId()));
            if (message.getCaption() != null)
            {
                sendDocument.setCaption(message.getCaption() +
                        "\n\n\n____________\nПоследние сообщения : " + lastTime +
                        "\nСессия: #c" + complaint.getSessionNum() +                          //Sessiya
                        "\nТема: #т" + complaint.getThemeNum() +
                        "\nЧат: #c" + complaint.getSessionNum() + "т" + complaint.getThemeNum());
            } else
                sendDocument.setCaption("____________\nПоследние сообщения : " + lastTime +
                        "\nСессия: #c" + complaint.getSessionNum() +                          //Sessiya
                        "\nТема: #т" + complaint.getThemeNum() +
                        "\nЧат: #c" + complaint.getSessionNum() + "т" + complaint.getThemeNum());

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
            List<InlineKeyboardButton> row = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText("Написать ответ");
            button.setCallbackData("reply-to-patient" + patientChatId);
            row.add(button);
            keyboard.add(row);
            markup.setKeyboard(keyboard);
            sendDocument.setReplyMarkup(markup);

            execute(sendDocument);
        } else if (message.hasAudio())
        {
            SendAudio sendAudio = new SendAudio();
            sendAudio.setChatId(String.valueOf(doctorChatId));
            sendAudio.setAudio(new InputFile(message.getAudio().getFileId()));

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
            List<InlineKeyboardButton> row1 = new ArrayList<>();
            InlineKeyboardButton button1 = new InlineKeyboardButton();

            button1.setText("Написать ответ");
            button1.setCallbackData("reply-to-patient" + patientChatId);
            row1.add(button1);
            keyboard.add(row1);
            markup.setKeyboard(keyboard);
            sendAudio.setReplyMarkup(markup);

            if (message.getCaption() != null)
            {
                sendAudio.setCaption(message.getCaption() +
                        "\n\n\n____________\nПоследние сообщения : " + lastTime +
                        "\nСессия: #c" + complaint.getSessionNum() +                          //Sessiya
                        "\nТема: #т" + complaint.getThemeNum() +
                        "\nЧат: #c" + complaint.getSessionNum() + "т" + complaint.getThemeNum());
            } else
                sendAudio.setCaption("____________\nПоследние сообщения : " + lastTime +
                        "\nСессия: #c" + complaint.getSessionNum() +                          //Sessiya
                        "\nТема: #т" + complaint.getThemeNum() +
                        "\nЧат: #c" + complaint.getSessionNum() + "т" + complaint.getThemeNum());


            execute(sendAudio);
        } else if (message.hasSticker())
        {
            SendSticker sendSticker = new SendSticker();
            sendSticker.setChatId(String.valueOf(doctorChatId));
            sendSticker.setSticker(new InputFile(message.getSticker().getFileId()));

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
            List<InlineKeyboardButton> row = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText("Написать ответ");
            button.setCallbackData("reply-to-patient" + patientChatId);
            row.add(button);
            keyboard.add(row);
            markup.setKeyboard(keyboard);
            sendSticker.setReplyMarkup(markup);

            execute(sendSticker);

        } else if (message.hasVideo())
        {
            SendVideo sendVideo = new SendVideo();
            sendVideo.setChatId(String.valueOf(doctorChatId));
            sendVideo.setVideo(new InputFile(message.getVideo().getFileId()));

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
            List<InlineKeyboardButton> row = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText("Написать ответ");
            button.setCallbackData("reply-to-patient" + patientChatId);
            row.add(button);
            keyboard.add(row);
            markup.setKeyboard(keyboard);
            sendVideo.setReplyMarkup(markup);


            if (message.getCaption() != null)
            {
                sendVideo.setCaption(message.getCaption() +
                        "\n\n\n____________\nПоследние сообщения : " + lastTime +
                        "\nСессия: #c" + complaint.getSessionNum() +                          //Sessiya
                        "\nТема: #т" + complaint.getThemeNum() +
                        "\nЧат: #c" + complaint.getSessionNum() + "т" + complaint.getThemeNum());
            } else
                sendVideo.setCaption("\n____________\nПоследние сообщения : " + lastTime +
                        "\nСессия: #c" + complaint.getSessionNum() +                          //Sessiya
                        "\nТема: #т" + complaint.getThemeNum() +
                        "\nЧат: #c" + complaint.getSessionNum() + "т" + complaint.getThemeNum());

            execute(sendVideo);

        } else if (message.hasLocation())
        {
            SendLocation sendLocation = new SendLocation();
            sendLocation.setChatId(String.valueOf(doctorChatId));
            sendLocation.setLatitude(message.getLocation().getLatitude());
            sendLocation.setLongitude(message.getLocation().getLongitude());
            sendLocation.setHeading(message.getLocation().getHeading());
            sendLocation.setHorizontalAccuracy(message.getLocation().getHorizontalAccuracy());
            sendLocation.setProximityAlertRadius(message.getLocation().getProximityAlertRadius());

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
            List<InlineKeyboardButton> row = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText("Написать ответ");
            button.setCallbackData("reply-to-patient" + patientChatId);
            row.add(button);
            keyboard.add(row);
            markup.setKeyboard(keyboard);
            sendLocation.setReplyMarkup(markup);

            execute(sendLocation);
        } else if (message.hasVoice())
        {
            SendVoice sendVoice = new SendVoice();
            sendVoice.setChatId(String.valueOf(doctorChatId));
            sendVoice.setVoice(new InputFile(message.getVoice().getFileId()));

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
            List<InlineKeyboardButton> row = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText("Написать ответ");
            button.setCallbackData("reply-to-patient" + patientChatId);
            row.add(button);
            keyboard.add(row);
            markup.setKeyboard(keyboard);
            sendVoice.setReplyMarkup(markup);

            if (message.getCaption() != null)
            {
                sendVoice.setCaption(message.getCaption() +
                        "\n\n\n____________\nПоследние сообщения : " + lastTime +
                        "\nСессия: #c" + complaint.getSessionNum() +                          //Sessiya
                        "\nТема: #т" + complaint.getThemeNum() +
                        "\nЧат: #c" + complaint.getSessionNum() + "т" + complaint.getThemeNum());
            } else
                sendVoice.setCaption("\n\n\n____________\nПоследние сообщения : " + lastTime +
                        "\nСессия: #c" + complaint.getSessionNum() +                          //Sessiya
                        "\nТема: #т" + complaint.getThemeNum() +
                        "\nЧат: #c" + complaint.getSessionNum() + "т" + complaint.getThemeNum());

            execute(sendVoice);
        } else
        {
            ForwardMessage forwardMessage = new ForwardMessage();
            forwardMessage.setChatId(String.valueOf(doctorChatId));
            forwardMessage.setFromChatId(String.valueOf(message.getChatId()));
            forwardMessage.setMessageId(message.getMessageId());
            execute(forwardMessage);

            SendMessage sm = new SendMessage();
            sm.setChatId(String.valueOf(doctorChatId));
            sm.setText("Нажмите кнопку ниже, чтобы написать ответ" +
                    "\n\n\n____________\nПоследние сообщения : " + lastTime +
                    "\nСессия: #c" + complaint.getSessionNum() +                          //Sessiya
                    "\nТема: #т" + complaint.getThemeNum() +
                    "\nЧат: #c" + complaint.getSessionNum() + "т" + complaint.getThemeNum());
            sm.setReplyToMessageId(message.getMessageId());

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
            List<InlineKeyboardButton> row1 = new ArrayList<>();
            InlineKeyboardButton button1 = new InlineKeyboardButton();

            button1.setText("напишите ответ");

            button1.setCallbackData("reply-to-patient" + patientChatId);
            row1.add(button1);
            keyboard.add(row1);
            markup.setKeyboard(keyboard);
            sm.setReplyMarkup(markup);

            execute(sm);
        }


        SendMessage patientSendMessage = new SendMessage();
        patientSendMessage.setChatId(patientChatId);

        if (patientFromDb.getLanguage().equals(PatientLanguage.UZ))
            patientSendMessage.setText("Xabaringiz yuborildi");
        else
            patientSendMessage.setText("Ваше сообщение было отправлено");

        execute(patientSendMessage);
        patientFromDb.setState(PatientState.WAIT_ANSWER_OF_REPLY_MESSAGE);
        patientFromDb.setLastMessageDate(new Date());
        patientRepo.save(patientFromDb);
    }

    private void patientReChooseMessageRequest(Update update) throws TelegramApiException
    {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        Long chatId = callbackQuery.getMessage().getChatId();

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

            if (fromDb.getLanguage().equals(PatientLanguage.UZ))
                button.setText(doctor.getSpecialityUz());
            else if (fromDb.getLanguage().equals(PatientLanguage.RU))
                button.setText(doctor.getSpecialityRu());

            button.setCallbackData("doctor-id" + doctor.getId().toString());
            row.add(button);
            inlineButtons.add(row);
        }

        inlineKeyboardMarkup.setKeyboard(inlineButtons);
        sendMessage.setReplyMarkup(inlineKeyboardMarkup);

        execute(sendMessage);

        fromDb.setState(PatientState.CHOOSE_DOCTOR);
        patientRepo.save(fromDb);

    }


    //TODO ==========================================================================================
    //TODO ==========================================================================================
    //TODO ==========================================================================================


    private void doctorMessageHandler(Update update) throws TelegramApiException
    {
        Message message = update.getMessage();
        Long doctorChatId = message.getChatId();
        Doctor doctor = doctorRepo.findByChatId(doctorChatId);


        if (message.hasText() && message.getText().equals("/start"))
        {
            doctor.setState(DoctorState.START);
            doctorRepo.save(doctor);
        }

        switch (doctor.getState())
        {
            case START:
                doctorStartHandler(update);
                break;
            case WRITING_ANSWER_FOR_COMPLAINT:
                doctorComplaintAnswerHandler(update);
                break;
            case WRITE_REPLY_MESSAGE_TO_PATIENT:
                doctorReplyMessageToPatientHandler(update);

        }

    }

    private void doctorCallBackQueryHandler(Update update) throws TelegramApiException
    {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        Long doctorChatId = callbackQuery.getMessage().getChatId();

        Doctor fromDb = doctorRepo.findByChatId(doctorChatId);

        String data = callbackQuery.getData();

        if (data.startsWith("ok") || data.startsWith("cancel"))
            doctorComplaintAcceptCallBackHandler(update);
        else if (data.startsWith("reply-to-patient"))
            doctorAnswerRequestForReplyMessage(update);
    }

    // TODO ------------------------------------------------------------------------

    private void doctorStartHandler(Update update) throws TelegramApiException
    {
        Message message = update.getMessage();
        Long doctorChatId = message.getChatId();

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(doctorChatId);
        sendMessage.setText("Добро пожаловать на страницу врача \n" +
                "Если вы получите какое-либо сообщение от пациентов, оно появится здесь.");
        execute(sendMessage);
    }


    private void doctorComplaintAcceptCallBackHandler(Update update) throws TelegramApiException
    {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        Long doctorChatId = callbackQuery.getMessage().getChatId();

        String data = callbackQuery.getData();

        Doctor doctorFromDb = doctorRepo.findByChatId(doctorChatId);

        Long complaintId = null;

        if (data.contains("ok"))
            complaintId = Long.valueOf(data.replaceAll("ok", ""));
        else
            complaintId = Long.valueOf(data.replaceAll("cancel", ""));


        if (doctorFromDb.getCurrentComplaintId() != null && !doctorFromDb.getCurrentComplaintId().equals(complaintId))
        {
            Long currentComplaintId = doctorFromDb.getCurrentComplaintId();

            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(doctorChatId);
            sendMessage.setText("Пожалуйста, ответьте на это первым:\n" +
                            complaintRepo.findById(currentComplaintId).get().getMessage()
//                    +"\nСессия: #c" + complaintRepo.findById(complaintId).get().getWriterId()
            );

            execute(sendMessage);
            return;
        }

        if (data.contains("ok"))
        {

            Complaint complaint = complaintRepo.findById(complaintId).get();

            if (complaint.getStatus().equals(ComplaintStatus.REJECTED_BY_DOCTOR))
            {
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(doctorChatId);
                sendMessage.setText("Вы не можете ответить на это сообщение, вы уже отклонили это сообщение : \n" + complaint.getMessage()
//                        +"\nСессия: #c" + complaint.getWriterId()
                );
                execute(sendMessage);
                return;
            } else if (complaint.getStatus().equals(ComplaintStatus.DOCTOR_ACCEPTED_NOT_ANSWERED))
            {
                return;

            } else if (complaint.getStatus().equals(ComplaintStatus.ANSWERED))
            {
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(doctorChatId);
                sendMessage.setText("Вы уже ответили на это сообщение: \n" + complaint.getMessage()
//                        +"\nСессия: #c" + complaint.getWriterId()
                );
                execute(sendMessage);
                return;
            }

            complaint.setStatus(ComplaintStatus.DOCTOR_ACCEPTED_NOT_ANSWERED);
            complaintRepo.save(complaint);

            doctorFromDb.setState(DoctorState.WRITING_ANSWER_FOR_COMPLAINT);
            doctorFromDb.setCurrentComplaintId(complaintId);
            doctorRepo.save(doctorFromDb);

            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(doctorChatId);
            sendMessage.setText("Запишите свой ответ для пациента :\n" + complaint.getMessage()
//                    +"\nСессия: #c" + complaint.getWriterId()
            );

            execute(sendMessage);
        } else if (data.contains("cancel"))
        {
            Complaint complaint = complaintRepo.findById(complaintId).get();

            if (complaint.getStatus().equals(ComplaintStatus.DOCTOR_ACCEPTED_NOT_ANSWERED))
            {

                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(doctorChatId);
                sendMessage.setText("Сначала необходимо ответить на сообщение:\n" + complaint.getMessage()
//                        +"\nСессия: #c" + complaint.getWriterId()
                );
                execute(sendMessage);
                return;
            } else if (complaint.getStatus().equals(ComplaintStatus.REJECTED_BY_DOCTOR))
            {
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(doctorChatId);
                sendMessage.setText("Вы уже отклонили это сообщение:\n" + complaint.getMessage()
//                        +"\nСессия: #c" + complaint.getWriterId()
                );
                execute(sendMessage);
                return;

            } else if (complaint.getStatus().equals(ComplaintStatus.ANSWERED))
            {
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(doctorChatId);
                sendMessage.setText("Вы не можете отклонить это сообщение, вы уже ответили:\n" + complaint.getMessage()
//                        +"\nСессия: #c" + complaint.getWriterId()
                );
                execute(sendMessage);
                return;
            }

            complaint.setStatus(ComplaintStatus.REJECTED_BY_DOCTOR);
            complaintRepo.save(complaint);

            Patient patient = patientRepo.findById(complaint.getWriterId()).get();
            patient.setComplaint(null);
            patient.setState(PatientState.START);  // TODO----------------------------------------------------
            patientRepo.save(patient);
            SendMessage patientMessage = new SendMessage();

            patientMessage.setChatId(patient.getChatId());

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

            List<InlineKeyboardButton> row1 = new ArrayList<>();
            InlineKeyboardButton button1 = new InlineKeyboardButton();

            if (patient.getLanguage().equals(PatientLanguage.RU))
            {
                patientMessage.setText(complaint.getMessage() + "\n -Извини , Врач не может ответить на ваш вопрос");
                button1.setText("Подать новую заявку");
            } else if (patient.getLanguage().equals(PatientLanguage.UZ))
            {
                patientMessage.setText(complaint.getMessage() + "\n -Kechirasiz, shifokor sizning savolingizga javob bera olmaydi");
                button1.setText("Yangi murojaat qoldirish");
            }

            button1.setCallbackData("patient-re-choose-doctor");
            row1.add(button1);
            keyboard.add(row1);
            markup.setKeyboard(keyboard);
            patientMessage.setReplyMarkup(markup);

            doctorFromDb.setCurrentComplaintId(null);
            doctorFromDb.setState(DoctorState.START);
            doctorRepo.save(doctorFromDb);

            execute(patientMessage);

            SendMessage doctorMessage = new SendMessage();
            doctorMessage.setChatId(doctorChatId);
            doctorMessage.setText("Этот запрос пациента был отменен : \n" + complaint.getMessage()
//                    +"\nСессия: #c" + complaint.getWriterId()
            );
            execute(doctorMessage);
        }

    }


    private void doctorComplaintAnswerHandler(Update update) throws TelegramApiException
    {
        Message message = update.getMessage();
        Long doctorChatId = message.getChatId();
        Doctor doctor = doctorRepo.findByChatId(doctorChatId);

        String answerOfDoctor = message.getText();
        Long currentComplaintId = doctor.getCurrentComplaintId();

        Complaint complaint = complaintRepo.findById(currentComplaintId).get();
        complaint.setAnswerOfDoctor(answerOfDoctor);
        complaint.setStatus(ComplaintStatus.ANSWERED);
        complaintRepo.save(complaint);

        SendMessage patientMessage = new SendMessage();
        Patient patient = patientRepo.findById(complaint.getWriterId()).get();
//        patient.setComplaint(null);
        patient.setState(PatientState.ANSWER_RECIVED);
        patientRepo.save(patient);

        doctor.setCurrentComplaintId(null);
        doctor.setState(DoctorState.START);
        doctorRepo.save(doctor);

        patientMessage.setChatId(patient.getChatId());
        if (patient.getLanguage().equals(PatientLanguage.RU))
            patientMessage.setText("Ответ врача " + doctor.getSpecialityRu() + " :\n" + answerOfDoctor);
        else
            patientMessage.setText(doctor.getSpecialityUz() + " javobi:\n" + answerOfDoctor);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton button1 = new InlineKeyboardButton();
        InlineKeyboardButton button2 = new InlineKeyboardButton();


        if (patient.getLanguage().equals(PatientLanguage.UZ))
        {
            button1.setText("Shifokorga xabar yozish");
            button2.setText("Yangi murojaat qoldrish");
        } else if (patient.getLanguage().equals(PatientLanguage.RU))
        {
            button1.setText("Напишите сообщение врачу");
            button2.setText("Подать новую заявку");
        }

        button1.setCallbackData("reply-message-to-doctor" + doctor.getChatId());
        button2.setCallbackData("patient-re-choose-doctor");
        row1.add(button1);
        row2.add(button2);

        keyboard.add(row1);
        keyboard.add(row2);

        markup.setKeyboard(keyboard);
        patientMessage.setReplyMarkup(markup);


        execute(patientMessage);


        SendMessage doctorMessage = new SendMessage();
        doctorMessage.setChatId(doctorChatId);
        doctorMessage.setText("Спасибо за ответ, ваш ответ доставлен");
        execute(doctorMessage);
    }


    private void doctorAnswerRequestForReplyMessage(Update update) throws TelegramApiException
    {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        Long doctorChatId = callbackQuery.getMessage().getChatId();
        String data = callbackQuery.getData();
        if (data.contains("reply-to-patient"))
        {
            Long patientChatId = Long.parseLong(data.replaceAll("reply-to-patient", ""));

            Patient patientFromDb = patientRepo.findByChatId(patientChatId);

            SendMessage sendMessageDoctor = new SendMessage();
            sendMessageDoctor.setChatId(doctorChatId);
            sendMessageDoctor.setText("Отправьте пациенту сообщение любого типа (файл, фото, голос) : " + patientFromDb.getFullName()
//                    +"\nСессия: #c" + patientFromDb.getId()
            );
            execute(sendMessageDoctor);

            Doctor doctorFromDb = doctorRepo.findByChatId(doctorChatId);
            doctorFromDb.setState(DoctorState.WRITE_REPLY_MESSAGE_TO_PATIENT);
            doctorFromDb.setCurrentReplyPatientChatId(patientChatId);
            doctorRepo.save(doctorFromDb);
        }
    }

    private void doctorReplyMessageToPatientHandler(Update update) throws TelegramApiException
    {
        Message message = update.getMessage();
        Long doctorChatId = message.getChatId();
        Doctor doctorFromDb = doctorRepo.findByChatId(doctorChatId);

        Long currentReplyPatientChatId = doctorFromDb.getCurrentReplyPatientChatId();
        Patient patientFromDb = patientRepo.findByChatId(currentReplyPatientChatId);

        SendMessage sendMessage = new SendMessage();

        Long patientChatId = patientFromDb.getChatId();


        sendMessage.setChatId(patientChatId);
        if (patientFromDb.getLanguage().equals(PatientLanguage.UZ))
        {
            sendMessage.setText("Shifokor javobi:");
        } else if (patientFromDb.getLanguage().equals(PatientLanguage.RU))
        {
            sendMessage.setText("Ответ врача:");
        }
        execute(sendMessage);

        if (message.hasText())
        {
            SendMessage sendMessageToPatient = new SendMessage();
            sendMessageToPatient.setChatId(patientChatId);
            sendMessageToPatient.setText(message.getText());

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
            List<InlineKeyboardButton> row1 = new ArrayList<>();
            InlineKeyboardButton button1 = new InlineKeyboardButton();

            if (patientFromDb.getLanguage().equals(PatientLanguage.UZ))
                button1.setText("Yangi murojaat qoldirish");
            else if (patientFromDb.getLanguage().equals(PatientLanguage.RU))
                button1.setText("Подать новую заявку");

            button1.setCallbackData("patient-re-choose-doctor");
            row1.add(button1);
            keyboard.add(row1);
            markup.setKeyboard(keyboard);
            sendMessageToPatient.setReplyMarkup(markup);

            execute(sendMessageToPatient);
        } else if (message.hasPhoto())
        {
            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setChatId(String.valueOf(patientChatId));
            sendPhoto.setPhoto(new InputFile(message.getPhoto().get(0).getFileId()));
            if (message.getCaption() != null)
                sendPhoto.setCaption(message.getCaption());

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
            List<InlineKeyboardButton> row1 = new ArrayList<>();
            InlineKeyboardButton button1 = new InlineKeyboardButton();

            if (patientFromDb.getLanguage().equals(PatientLanguage.UZ))
                button1.setText("Yangi murojaat qoldirish");
            else if (patientFromDb.getLanguage().equals(PatientLanguage.RU))
                button1.setText("Подать новую заявку");

            button1.setCallbackData("patient-re-choose-doctor");
            row1.add(button1);
            keyboard.add(row1);
            markup.setKeyboard(keyboard);
            sendPhoto.setReplyMarkup(markup);
            execute(sendPhoto);
        } else if (message.hasDocument())
        {
            SendDocument sendDocument = new SendDocument();
            sendDocument.setChatId(String.valueOf(patientChatId));
            sendDocument.setDocument(new InputFile(message.getDocument().getFileId()));
            if (message.getCaption() != null)
            {
                sendDocument.setCaption(message.getCaption());
            }

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
            List<InlineKeyboardButton> row1 = new ArrayList<>();
            InlineKeyboardButton button1 = new InlineKeyboardButton();

            if (patientFromDb.getLanguage().equals(PatientLanguage.UZ))
                button1.setText("Yangi murojaat qoldirish");
            else if (patientFromDb.getLanguage().equals(PatientLanguage.RU))
                button1.setText("Подать новую заявку");

            button1.setCallbackData("patient-re-choose-doctor");
            row1.add(button1);
            keyboard.add(row1);
            markup.setKeyboard(keyboard);
            sendDocument.setReplyMarkup(markup);

            execute(sendDocument);
        } else if (message.hasAudio())
        {
            SendAudio sendAudio = new SendAudio();
            sendAudio.setChatId(String.valueOf(patientChatId));
            sendAudio.setAudio(new InputFile(message.getAudio().getFileId()));

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
            List<InlineKeyboardButton> row1 = new ArrayList<>();
            InlineKeyboardButton button1 = new InlineKeyboardButton();

            if (patientFromDb.getLanguage().equals(PatientLanguage.UZ))
                button1.setText("Yangi murojaat qoldirish");
            else if (patientFromDb.getLanguage().equals(PatientLanguage.RU))
                button1.setText("Подать новую заявку");

            button1.setCallbackData("patient-re-choose-doctor");
            row1.add(button1);
            keyboard.add(row1);
            markup.setKeyboard(keyboard);
            sendAudio.setReplyMarkup(markup);

            execute(sendAudio);
        } else if (message.hasSticker())
        {
            SendSticker sendSticker = new SendSticker();
            sendSticker.setChatId(String.valueOf(patientChatId));
            sendSticker.setSticker(new InputFile(message.getSticker().getFileId()));

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
            List<InlineKeyboardButton> row1 = new ArrayList<>();
            InlineKeyboardButton button1 = new InlineKeyboardButton();

            if (patientFromDb.getLanguage().equals(PatientLanguage.UZ))
                button1.setText("Yangi murojaat qoldirish");
            else if (patientFromDb.getLanguage().equals(PatientLanguage.RU))
                button1.setText("Подать новую заявку");

            button1.setCallbackData("patient-re-choose-doctor");
            row1.add(button1);
            keyboard.add(row1);
            markup.setKeyboard(keyboard);
            sendSticker.setReplyMarkup(markup);

            execute(sendSticker);
        } else if (message.hasVideo())
        {
            SendVideo sendVideo = new SendVideo();
            sendVideo.setChatId(String.valueOf(patientChatId));
            sendVideo.setVideo(new InputFile(message.getVideo().getFileId()));

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
            List<InlineKeyboardButton> row1 = new ArrayList<>();
            InlineKeyboardButton button1 = new InlineKeyboardButton();

            if (patientFromDb.getLanguage().equals(PatientLanguage.UZ))
                button1.setText("Yangi murojaat qoldirish");
            else if (patientFromDb.getLanguage().equals(PatientLanguage.RU))
                button1.setText("Подать новую заявку");

            button1.setCallbackData("patient-re-choose-doctor");
            row1.add(button1);
            keyboard.add(row1);
            markup.setKeyboard(keyboard);
            sendVideo.setReplyMarkup(markup);
            execute(sendVideo);

        } else if (message.hasLocation())
        {
            SendLocation sendLocation = new SendLocation();
            sendLocation.setChatId(String.valueOf(patientChatId));
            sendLocation.setLatitude(message.getLocation().getLatitude());
            sendLocation.setLongitude(message.getLocation().getLongitude());
            sendLocation.setHeading(message.getLocation().getHeading());
            sendLocation.setHorizontalAccuracy(message.getLocation().getHorizontalAccuracy());
            sendLocation.setProximityAlertRadius(message.getLocation().getProximityAlertRadius());

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
            List<InlineKeyboardButton> row1 = new ArrayList<>();
            InlineKeyboardButton button1 = new InlineKeyboardButton();

            if (patientFromDb.getLanguage().equals(PatientLanguage.UZ))
                button1.setText("Yangi murojaat qoldirish");
            else if (patientFromDb.getLanguage().equals(PatientLanguage.RU))
                button1.setText("Подать новую заявку");

            button1.setCallbackData("patient-re-choose-doctor");
            row1.add(button1);
            keyboard.add(row1);
            markup.setKeyboard(keyboard);
            sendLocation.setReplyMarkup(markup);
            execute(sendLocation);

        } else if (message.hasVoice())
        {
            SendVoice sendVoice = new SendVoice();
            sendVoice.setChatId(String.valueOf(patientChatId));
            sendVoice.setVoice(new InputFile(message.getVoice().getFileId()));

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
            List<InlineKeyboardButton> row1 = new ArrayList<>();
            InlineKeyboardButton button1 = new InlineKeyboardButton();

            if (patientFromDb.getLanguage().equals(PatientLanguage.UZ))
                button1.setText("Yangi murojaat qoldirish");
            else if (patientFromDb.getLanguage().equals(PatientLanguage.RU))
                button1.setText("Подать новую заявку");

            button1.setCallbackData("patient-re-choose-doctor");
            row1.add(button1);
            keyboard.add(row1);
            markup.setKeyboard(keyboard);
            sendVoice.setReplyMarkup(markup);
            execute(sendVoice);

        } else
        {
            ForwardMessage forwardMessage = new ForwardMessage();
            forwardMessage.setChatId(String.valueOf(patientChatId));
            forwardMessage.setFromChatId(String.valueOf(message.getChatId()));
            forwardMessage.setMessageId(message.getMessageId());

            execute(forwardMessage);


            SendMessage sm = new SendMessage();
            sm.setChatId(String.valueOf(patientChatId));
            if (patientFromDb.getLanguage().equals(PatientLanguage.UZ))
                sm.setText("Yangi murojaat qoldirish uchun pastdagi tugmani bosing");
            else if (patientFromDb.getLanguage().equals(PatientLanguage.RU))
                sm.setText("Нажмите кнопку ниже, чтобы оставить новый запрос");

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
            List<InlineKeyboardButton> row1 = new ArrayList<>();
            InlineKeyboardButton button1 = new InlineKeyboardButton();

            if (patientFromDb.getLanguage().equals(PatientLanguage.UZ))
                button1.setText("Yangi murojaat qoldirish");
            else if (patientFromDb.getLanguage().equals(PatientLanguage.RU))
                button1.setText("Подать новую заявку");

            button1.setCallbackData("patient-re-choose-doctor");
            row1.add(button1);
            keyboard.add(row1);
            markup.setKeyboard(keyboard);
            sm.setReplyMarkup(markup);

            execute(sm);
        }

        SendMessage sendMessageDoctor = new SendMessage();
        sendMessageDoctor.setChatId(doctorChatId);
        sendMessageDoctor.setText("Сообщение доставлено"
//                +"\nСессия: #c" + patientFromDb.getId()
        );
        execute(sendMessageDoctor);


        doctorFromDb.setState(DoctorState.START);
        doctorFromDb.setCurrentReplyPatientChatId(null);
        doctorRepo.save(doctorFromDb);
    }
}
