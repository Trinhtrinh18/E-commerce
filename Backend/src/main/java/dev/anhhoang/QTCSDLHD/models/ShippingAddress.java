// src/main/java/dev/anhhoang/QTCSDLHD/models/ShippingAddress.java
package dev.anhhoang.QTCSDLHD.models;

import lombok.Data;
import org.springframework.data.annotation.Id;

@Data
public class ShippingAddress {
    @Id
    private String addressId; // Using String for MongoDB ObjectId
    private String receiverName;
    private String receiverPhone;
    private String street;
    private String ward;
    private String district;
    private String city;
    private boolean isDefault;
}
