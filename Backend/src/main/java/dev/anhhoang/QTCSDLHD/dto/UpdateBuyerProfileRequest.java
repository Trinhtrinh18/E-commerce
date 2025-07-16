package dev.anhhoang.QTCSDLHD.dto;

import dev.anhhoang.QTCSDLHD.models.Address;
import dev.anhhoang.QTCSDLHD.models.BankAccount;
import lombok.Data;

@Data
public class UpdateBuyerProfileRequest {
    private String phoneNumber;
    private Address primaryAddress;
    private BankAccount bankAccount;
}