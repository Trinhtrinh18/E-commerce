// src/main/java/dev/anhhoang/QTCSDLHD/models/User.java
package dev.anhhoang.QTCSDLHD.models;

import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.Data;

@Document(collection = "users") // This maps the class to the "users" collection in MongoDB
@Data
public class User {

    @Id
    private String id; // MongoDB will automatically generate this

    @Indexed(unique = true) // Ensures no two users can have the same email
    private String email;

    @Field("password") // Explicitly mapping field name, though it's not required if same
    private String password; // This will store the HASHED password

    @Field("full_name")
    private String fullName;

    private Set<Role> roles; // A set of roles, e.g., {ROLE_BUYER} or {ROLE_BUYER, ROLE_SELLER}

    @Field("buyer_profile")
    private BuyerProfile buyerProfile; // This will be null if the user is not a buyer

    @Field("seller_profile")
    private SellerProfile sellerProfile; // This will be null if the user is not a seller

    public SellerProfile getSellerProfile() {
        return sellerProfile;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public String getFullName() {
        return fullName;
    }
}