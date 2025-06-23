package com.example.customer_service.ultils;

import java.util.regex.Pattern;

public class ValidationUtils {

    // Email phải có định dạng hợp lệ, không giới hạn chỉ @gmail.com
    public static boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$";
        Pattern pattern = Pattern.compile(emailRegex);
        return email != null && pattern.matcher(email).matches();
    }

    // Số điện thoại Việt Nam hợp lệ (bắt đầu bằng 03, 05, 07, 08, 09 và theo sau là 8 số)
    public static boolean isValidPhoneNumber(String phoneNumber) {
        String phoneRegex = "^(0)(3[2-9]|5[6|8|9]|7[0|6-9]|8[1-5]|9[0-9])\\d{7}$";
        Pattern pattern = Pattern.compile(phoneRegex);
        return phoneNumber != null && pattern.matcher(phoneNumber).matches();
    }

    // Mật khẩu mạnh: ít nhất 6 ký tự, có ít nhất 1 chữ hoa, 1 chữ thường, 1 số, 1 ký tự đặc biệt
    public static boolean isValidPassword(String password) {
        String passwordRegex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&+=!]).{6,}$";
        Pattern pattern = Pattern.compile(passwordRegex);
        return password != null && pattern.matcher(password).matches();
    }
}
