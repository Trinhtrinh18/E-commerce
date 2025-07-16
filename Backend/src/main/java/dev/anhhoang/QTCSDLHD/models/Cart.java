package dev.anhhoang.QTCSDLHD.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "carts")
@Data
public class Cart {
    @Id
    private String _id;
    private String customerId;
    private List<CartItem> items;
    private String status; // active, checked_out, etc.
    private LocalDateTime created_at;
    private LocalDateTime updated_at;
}