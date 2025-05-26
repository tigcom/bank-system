package com.example.transaction_service.enums;

import com.example.transaction_service.exception.AppException;
import com.example.transaction_service.exception.ErrorCode;

import java.util.Arrays;

public enum BankCode {
    KIENLONGBANK("970452", "Ngân hàng TMCP Kiên Long (KienLong Bank)"),
    VIETCOMBANK("970436", "Ngân hàng TMCP Ngoại thương Việt Nam (Vietcombank)"),
    VIETINBANK("970415", "Ngân hàng TMCP Công thương Việt Nam (VietinBank)"),
    BIDV("970418", "Ngân hàng TMCP Đầu tư và Phát triển Việt Nam (BIDV)"),
    AGRIBANK("970405", "Ngân hàng Nông nghiệp và Phát triển Nông thôn Việt Nam (Agribank)"),
    ACB("970416", "Ngân hàng TMCP Á Châu (ACB)"),
    TECHCOMBANK("970407", "Ngân hàng TMCP Kỹ Thương Việt Nam (Techcombank)"),
    SACOMBANK("970403", "Ngân hàng TMCP Sài Gòn Thương Tín (Sacombank)"),
    VPBANK("970432", "Ngân hàng TMCP Việt Nam Thịnh Vượng (VPBank)"),
    MB("970422", "Ngân hàng TMCP Quân đội (MB Bank)"),
    TPBANK("970423", "Ngân hàng TMCP Tiên Phong (TPBank)"),
    SHB("970443", "Ngân hàng TMCP Sài Gòn – Hà Nội (SHB)"),
    HDBANK("970437", "Ngân hàng TMCP Phát triển TP. HCM (HDBank)"),
    VIB("970441", "Ngân hàng TMCP Quốc tế Việt Nam (VIB)"),
    SCB("970429", "Ngân hàng TMCP Sài Gòn (SCB)"),
    OCB("970448", "Ngân hàng TMCP Phương Đông (OCB)"),
    LIENVIETPOSTBANK("970404", "Ngân hàng TMCP Bưu Điện Liên Việt (LienVietPostBank)"),
    PVCOMBANK("970409", "Ngân hàng TMCP Đại Chúng Việt Nam (PVcomBank)"),
    VIETCAPITALBANK("970434", "Ngân hàng TMCP Bản Việt (Viet Capital Bank)"),
    PGBANK("970411", "Ngân hàng TMCP Xăng dầu Petrolimex (PG Bank)"),
    WESTERNBANK("970425", "Ngân hàng TMCP Phương Tây (Western Bank)"),
    SAIGONBANK("970428", "Ngân hàng TMCP Sài Gòn Công Thương (Saigonbank)"),
    BACABANK("970427", "Ngân hàng TMCP Bắc Á (BacABank)"),
    NAMA("970440", "Ngân hàng TMCP Nam Á (Nam A Bank)"),
    MHB("970408", "Ngân hàng TMCP Phát Triển Nhà Đồng Bằng Sông Cửu Long (MHB)"),
    EXIMBANK("970420", "Ngân hàng TMCP Xuất Nhập Khẩu Việt Nam (Eximbank)"),
    SHBVN("970455", "Ngân hàng TMCP Sài Gòn Hà Nội (SHB VN)"),
    SAIGONHABANK("970446", "Ngân hàng TMCP Sài Gòn Hà Nội (Saigonbank Hà Nội)"),
    NVBANK("970414", "Ngân hàng TMCP Nam Việt (NamViet Bank)"),
    OCEANBANK("970426", "Ngân hàng TMCP Đại Dương (OceanBank)"),
    PGBANK_OLD("970412", "Ngân hàng TMCP Xăng dầu Petrolimex (PG Bank - cũ)"),
    DONGABANK("970410", "Ngân hàng TMCP Đông Á (DongA Bank)"),
    MSB("970439", "Ngân hàng TMCP Hàng Hải Việt Nam (MSB)"),
    BACVIETBANK("970413", "Ngân hàng TMCP Bảo Việt (BaoViet Bank)"),
    VIB_OLD("970442", "Ngân hàng TMCP Quốc tế Việt Nam (VIB - cũ)"),
    VCCB("970419", "Ngân hàng TMCP Xây dựng Việt Nam (VCCB)"),
    ABBANK("970431", "Ngân hàng TMCP An Bình (ABBANK)"),
    BAB("970424", "Ngân hàng TMCP Bắc Á (Bac A Bank)"),
    NONGHYUPBANK("970450", "Ngân hàng Nông nghiệp Hàn Quốc (NongHyup Bank - chi nhánh Việt Nam)");

    private final String code;
    private final String bankName;
    BankCode(String code, String bankName) {
        this.code = code;
        this.bankName = bankName;
    }

    public String getCode() { return code; }
    public String getBankName() { return bankName; }

    public static BankCode fromCode(String code) {
        return Arrays.stream(values())
                .filter(b -> b.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.BANK_CODE_VALID));
    }
}
