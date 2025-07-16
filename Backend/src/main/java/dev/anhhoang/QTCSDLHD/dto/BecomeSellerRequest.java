package dev.anhhoang.QTCSDLHD.dto;

import dev.anhhoang.QTCSDLHD.models.Address;
import dev.anhhoang.QTCSDLHD.models.BankAccount;
import dev.anhhoang.QTCSDLHD.models.BusinessType;
import lombok.Data;

@Data
public class BecomeSellerRequest {
    private String shopName;
    private String phoneNumber;
    private Address pickupAddress;
    private BankAccount bankAccount;

    // We can make the logo optional for the initial request
    private String shopLogoUrl;
    private BusinessType businessType;

}