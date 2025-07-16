// src/main/java/dev/anhhoang/QTCSDLHD/models/BankAccount.java
package dev.anhhoang.QTCSDLHD.models;

import lombok.Data;

@Data
public class BankAccount {
    private String bankName;
    private String accountHolder;
    private String accountNumber;
}