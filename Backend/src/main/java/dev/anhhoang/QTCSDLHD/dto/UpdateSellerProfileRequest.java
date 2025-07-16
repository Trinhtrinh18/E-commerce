package dev.anhhoang.QTCSDLHD.dto;

import dev.anhhoang.QTCSDLHD.models.Address;
import dev.anhhoang.QTCSDLHD.models.BankAccount;
import lombok.Data;

@Data
public class UpdateSellerProfileRequest {
    // DTO này chứa tất cả các trường người dùng có thể chỉnh sửa
    private String shopName;
    private String phoneNumber;
    private Address pickupAddress;
    private BankAccount bankAccount;
}