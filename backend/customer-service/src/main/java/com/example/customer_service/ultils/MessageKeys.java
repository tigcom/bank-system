package com.example.customer_service.ultils;

public class MessageKeys {

    // Login-related messages
    public static final String LOGIN_SUCCESSFULLY = "user.login.login_successfully";
    public static final String LOGIN_FAILED = "user.login.login_failed";
    public static final String USER_IS_LOCKED = "user.login.user_locked";
    public static final String USER_NOT_FOUND = "user.login.user_not_found";
    public static final String WRONG_PASSWORD = "user.login.wrong_password";
    public static final String INVALID_PHONE_PASSWORD = "user.login.invalid_phone_password";

    // Register-related messages
    public static final String REGISTER_FAILED = "user.register.register_failed";
    public static final String KAFKA_FAILED = "user.register.kafka_failed";
    public static final String REGISTER_SUCCESSFULLY = "user.register.register_successfully";
    public static final String USER_EXISTS = "user.register.username_exists";
    public static final String PHONE_EXISTS = "user.register.phone_exists";
    public static final String EMAIL_EXISTS = "user.register.email_exists";
    public static final String IDENTITY_NUMBER_EXISTS = "user.register.identity_number_exists";
    public static final String ROLE_NOT_FOUND = "user.register.role_not_found";
    public static final String CANNOT_REGISTER_ADMIN = "user.register.cannot_register_admin";

    // Validation messages for UserDTO & RegisterCustomerDTO
    public static final String NOT_BLANK_FULL_NAME = "validation.user.full_name.not_blank";
    public static final String NOT_BLANK_EMAIL = "validation.user.email.not_blank";
    public static final String USERNAME_SIZE = "validation.user.username.size";
    public static final String USERNAME_PATTERN = "validation.user.username.pattern";
    public static final String NOT_BLANK_ADDRESS = "validation.user.address.not_blank";
    public static final String NOT_BLANK_IDENTITY_NUMBER= "validation.user.identity_not_blank";
    public static final String IDENTITY_NUMBER_PATTERN = "validation.user.identity_number.pattern";
    public static final String INVALID_EMAIL = "validation.user.email.invalid";
    public static final String NOT_BLANK_PHONE_NUMBER = "validation.user.phone_number.not_blank";
    public static final String PHONE_NUMBER_INVALID = "validation.user.phone_number.invalid";
    public static final String NOT_BLANK_PASSWORD = "validation.user.password.not_blank";
    public static final String NOT_BLANK_CONFIRM_PASSWORD = "validation.user.confirm.password.not_blank";
    public static final String PASSWORD_PATTERN = "validation.user.password.pattern";
    public static final String NOT_NULL_DOB = "validation.user.date_of_birth.not_null";
    public static final String DOB_MUST_BE_PAST = "validation.user.date_of_birth.past";
    public static final String INVALID_GENDER = "validation.user.gender.invalid";
    public static final String NOT_NULL_ROLE_ID = "validation.user.role_id.not_null";

    // Validation messages for UserLoginDTO
    public static final String LOGIN_NOT_BLANK_USERNAME = "validation.login.username.not_blank";
    public static final String LOGIN_NOT_BLANK_PASSWORD = "validation.login.password.not_blank";

    // General error messages
    public static final String FAILED = "general.failed";
    public static final String INVALID_REQUEST = "error.invalid_request";
    public static final String INVALID_OTP = "error.invalid_otp";
    public static final String INVALID_KYC_DATA = "error.invalid_kyc_data";
    public static final String ACCOUNT_ALREADY_VERIFIED = "error.account_already_verified";
    public static final String KYC_MISMATCH_IDENTITY = "error.kyc_mismatch_identity";
    public static final String KYC_MISMATCH_NAME = "error.kyc_mismatch_name";
    public static final String KYC_MISMATCH_DOB = "error.kyc_mismatch_dob";
    public static final String KYC_MISMATCH_GENDER = "error.kyc_mismatch_gender";
    public static final String CLOSED_ACCOUNT_STATUS = "error.closed_account_status";
    public static final String ADMIN_ONLY = "error.admin_only";
    public static final String INVALID_CONFIRM_PASSWORD = "error.invalid_confirm_password";
    public static final String PASSWORD_TOO_SHORT = "error.password_too_short";
    public static final String INVALID_TOKEN = "error.invalid_token";
    public static final String EXPIRED_TOKEN = "error.expired_token";
    public static final String ACCOUNT_NOT_ACTIVE = "error.account_not_active";
    public static final String ACCOUNT_ERROR = "error.account";
    public static final String UNAUTHORIZED_ACCESS = "error.unauthorized_access";
    public static final String EMAIL_NOT_FOUND = "error.email_not_found";
    public static final String INVALID_STATUS = "error.invalid_status";
    public static final String KEYCLOAK_ERROR = "error.keycloak_error";
    public static final String KEYCLOAK_PARSE_ERROR = "error.keycloak_parse_error";
    public static final String KEYCLOAK_ROLE_FAILED = "error.keycloak_role_failed";
    public static final String KEYCLOAK_CREATE_FAILED = "error.keycloak_create_failed";
    public static final String KEYCLOAK_UPDATE_FAILED = "error.keycloak_update_failed";
    public static final String KEYCLOAK_USER_EMAIL_FAILED = "error.keycloak_user_email";
    public static final String KEYCLOAK_PASSWORD_UPDATE_FAILED = "error.keycloak_password_update_failed";
    public static final String CORE_BANKING_SYNC_FAILED = "error.core_banking_sync_failed";
    public static final String STATUS_SYNC_FAILED = "error.status_sync_failed";
    public static final String KYC_SYNC_FAILED = "error.kyc_sync_failed";
    public static final String KYC_VERIFICATION_FAILED = "error.kyc_verification_failed";
    public static final String OTP_SEND_FAILED = "error.otp_send_failed";
    public static final String EMAIL_SEND_FAILED = "error.email_send_failed";
    public static final String CUSTOMER_NOT_EXISTED = "error.customer_not_existed";

    // Success messages
    public static final String SUCCESS_UPDATE = "success.update";
    public static final String PASSWORD_RESET_SUCCESS = "success.password_reset";
    public static final String PASSWORD_UPDATED = "success.password_updated";
    public static final String STATUS_UPDATED = "success.status_updated";
    public static final String FORGOT_PASSWORD_LINK_SENT = "success.forgot_password_link_sent";
    public static final String FORGOT_PASSWORD_NOTIFICATION = "success.forgot_password_notification";

    public static final String SUCCESS_GET_CUSTOMER = "success.get.list.customer";

    // Email-related messages
    public static final String FORGOT_PASSWORD_SUBJECT = "mail.forgot_password_subject";
    public static final String FORGOT_PASSWORD_BODY = "mail.forgot_password_body";

    // OTP
    public static final String OTP_SENT = "otp.sent";
    public static final String OTP_BODY = "otp.body";

    // Keycloak
    public static final String KEYCLOAK_UNKNOWN = "keycloak.unknown";
}
