package uz.amon.bot;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import uz.amon.domain.dto.ComplaintDto;
import uz.amon.domain.dto.PatientDto;
import uz.amon.domain.enums.PatientState;
import uz.amon.domain.enums.Speciality;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BotService {




    public SendMessage sendStart(String chatId) {
        return new SendMessage(chatId, "Boshlash uchun /start buyrug'ini kiriting");
    }
    public SendMessage enterFirstname(String chatId) {
        return new SendMessage(chatId,"Ismingizni kiriting:");
    }

    public SendMessage enterLastName(String chatId) {
        return new SendMessage(chatId,"Familiyangizni kiriting:");
    }
    public SendMessage validationMessage(String chatId, String validationMessage) {
        return new SendMessage(chatId, validationMessage);
    }

    public SendMessage enterPhone(String chatId) {
        return new SendMessage(chatId, "Telefon raqamingizni kiriting");
    }

    public SendMessage switchSpeciality(String chatId) {

        SendMessage sendMessage=new SendMessage(chatId,"Shikoyatingizni qaysi doctorga yubormoqchisiz : ");
        sendMessage.setReplyMarkup(switchReplyMarkup());
        return sendMessage;
    }
    public SendMessage sendPhoto(String chatId) {
        return new SendMessage(chatId, "Photo yuboring:");
    }
    public SendMessage enterComplaintMessage(String chatid){
        return new SendMessage(chatid,"Kasallik tafsilotlarini yozing:");
    }
    public SendPhoto sendComplaintToDoctor(String chatId, PatientDto patientDto, ComplaintDto complaintDto,String fileId,String data){
        SendPhoto sendPhoto = new SendPhoto();

        sendPhoto.setChatId(chatId);
        sendPhoto.setPhoto(new InputFile(fileId));
        String caption = String.format(
                "Sizning malumotlaringiz: \nFirst Name: %s\n" +
                        "Last Name: %s\n" +
                        "Phone: %s\n" +
                        "Kasallik tafsilotlari: %s\n" +
                        "Siz bu ma'limotlarni %s ga yubormoqchimisiz "
                ,
                patientDto.getFirstname(),
                patientDto.getLastname(),
                patientDto.getPhone(),
                complaintDto.getMessage(),
                data


        );
        sendPhoto.setReplyMarkup(sendComplaintMarkup());

        sendPhoto.setCaption(caption);

        return sendPhoto;
    }
    public InlineKeyboardMarkup sendComplaintMarkup(){
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> actionRow = new ArrayList<>();
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText("Saqlash");
        button.setCallbackData("save");
        actionRow.add(button);
        rowsInline.add(actionRow);

        markupInline.setKeyboard(rowsInline);
        return markupInline;
    }
    public InlineKeyboardMarkup switchReplyMarkup(){
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = getLists();
        markupInline.setKeyboard(rowsInline);
        return markupInline;
    }


    private static List<List<InlineKeyboardButton>> getLists() {
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        List<InlineKeyboardButton> rowInline = null;

        for (int i = 0; i < Speciality.values().length; i++) {
            if (i % 2 == 0) {
                rowInline = new ArrayList<>();
            }

            Speciality value = Speciality.values()[i];
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(value.toString());
            button.setCallbackData(value.toString());
            rowInline.add(button);

            if (i % 2 == 1 || i == Speciality.values().length - 1) {
                rowsInline.add(rowInline);
            }
        }
        return rowsInline;
    }



}
