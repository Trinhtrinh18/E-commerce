package dev.anhhoang.QTCSDLHD.dto;

import dev.anhhoang.QTCSDLHD.models.BankAccount;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@Data
public class CreateOrderRequest {
    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Phone number is required")
    private String phoneNumber;

    @NotBlank(message = "Shipping address is required")
    private String shippingAddress;

    @NotBlank(message = "Payment method is required")
    private String paymentMethod;

    private BankAccount bankAccount;

    @NotEmpty(message = "Order items cannot be empty")
    private List<CartItemRequest> items;

    @JsonProperty("isBuyNow")
    private boolean isBuyNow = false; // Add this field, default to false
}