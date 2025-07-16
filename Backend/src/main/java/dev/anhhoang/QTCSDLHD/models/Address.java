// src/main/java/dev/anhhoang/QTCSDLHD/models/Address.java
package dev.anhhoang.QTCSDLHD.models;

import lombok.Data;

@Data // Lombok annotation to generate getters, setters, etc.
public class Address {
    private String street;
    private String ward;
    private String district;
    private String city;
}