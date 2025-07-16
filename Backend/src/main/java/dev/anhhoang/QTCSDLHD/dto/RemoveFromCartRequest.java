package dev.anhhoang.QTCSDLHD.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RemoveFromCartRequest {
    @NotBlank(message = "Product ID is required")
    private String productId;
}