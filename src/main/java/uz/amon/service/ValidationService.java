package uz.amon.service;

import org.springframework.stereotype.Service;

@Service
public class ValidationService {
    public String validateNames(String text, String textName) {
        if (!text.matches("^[A-Za-z]+$") || text.length() < 2 || text.length() > 50) {
            return textName + " faqat harflardan iborat bo'lishi kerak, va uzunligi 2 dan 50 gacha bo'lishi kerak.";
        }
        if (!Character.isUpperCase(text.charAt(0))) {
            return textName + " bosh harf bilan boshlanishi kerak.";
        }
        return null;
    }
    public String validatePhoneNumber(String phoneNumber) {
        // Regex to match Uzbek phone numbers: +998 followed by 9 digits
        String phoneRegex = "^\\+998[0-9]{9}$";
        if (!phoneNumber.matches(phoneRegex)) {
            return "Telefon raqami +998 bilan boshlanishi va 9 ta raqamdan iborat bo'lishi kerak.";
        }
        return null;
    }
}
