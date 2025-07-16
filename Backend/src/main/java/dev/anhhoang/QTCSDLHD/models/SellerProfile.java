package dev.anhhoang.QTCSDLHD.models;

import lombok.Data;
import org.springframework.data.mongodb.core.index.Indexed;

@Data
public class SellerProfile {

    @Indexed(unique = true) // Đảm bảo shopId là duy nhất trong toàn bộ database
    private String shopId;
    private BusinessType businessType; // Loại hình kinh doanh
    private String shopName;
    private String shopLogoUrl;
    private String phoneNumber;
    private Address pickupAddress;
    private BankAccount bankAccount;
}