package dev.anhhoang.QTCSDLHD.dto;

import dev.anhhoang.QTCSDLHD.models.BuyerProfile;
import dev.anhhoang.QTCSDLHD.models.Role;
import dev.anhhoang.QTCSDLHD.models.SellerProfile;
import lombok.Data;
import java.util.Set;

@Data
public class UserProfileResponse {
    private String id;
    private String email;
    private String fullName;
    private Set<Role> roles;
    private BuyerProfile buyerProfile;
    private SellerProfile sellerProfile; // This will be null if the user is not a seller
}