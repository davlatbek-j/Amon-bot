package uz.amon.service;

import org.springframework.stereotype.Service;

@Service
public class ValidationService {

//    Method name, lastname, surname uchun validatsiya. textName ga kirib kelishi mumkin(name, lastname, surname). textga esa
//    patient yozgan text kirib keladi.
    public String validateNames(String text, String textName) {
        if (!text.matches("^[A-Za-z]+$") || text.length() < 2 || text.length() > 50) {
            return textName + " faqat harflardan iborat bo'lishi kerak, va uzunligi 2 dan 50 gacha bo'lishi kerak.";
        }
        if (!Character.isUpperCase(text.charAt(0))) {
            return textName + " bosh harf bilan boshlanishi kerak";
        }
        return null;
    }

    public String validatePhoneNumber(String phoneNumber) {
        String phoneRegex = "^\\+998[0-9]{9}$";
        if (!phoneNumber.matches(phoneRegex)) {
            return "Telefon raqami +998 bilan boshlanishi va 9 ta raqamdan iborat bo'lishi kerak";
        }
        return null;
    }
}
