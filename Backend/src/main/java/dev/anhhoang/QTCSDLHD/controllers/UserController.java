package dev.anhhoang.QTCSDLHD.controllers;

import dev.anhhoang.QTCSDLHD.dto.BecomeSellerRequest;
import dev.anhhoang.QTCSDLHD.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import dev.anhhoang.QTCSDLHD.dto.UserProfileResponse;
import org.springframework.web.bind.annotation.GetMapping;
import java.security.Principal;
import dev.anhhoang.QTCSDLHD.dto.UpdateSellerProfileRequest;
import org.springframework.security.access.prepost.PreAuthorize; // <-- Thêm import này
import org.springframework.web.bind.annotation.*;
import dev.anhhoang.QTCSDLHD.dto.UpdateBuyerProfileRequest;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/become-seller")
    public ResponseEntity<?> becomeSeller(@RequestBody BecomeSellerRequest request, Principal principal) {
        try {
            // Bây giờ phương thức này trả về hồ sơ đã được cập nhật
            UserProfileResponse updatedProfile = userService.becomeSeller(principal.getName(), request);
            // Gửi hồ sơ đó về cho frontend
            return ResponseEntity.ok(updatedProfile);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUserProfile(Principal principal) {
        // principal.getName() gives us the email of the currently authenticated user
        UserProfileResponse userProfile = userService.findUserProfileByEmail(principal.getName());
        return ResponseEntity.ok(userProfile);
    }

    @PutMapping("/update-seller-profile")
    @PreAuthorize("hasRole('SELLER')") // Đảm bảo chỉ người bán mới có thể gọi API này
    public ResponseEntity<?> updateSellerProfile(@RequestBody UpdateSellerProfileRequest request, Principal principal) {
        try {
            userService.updateSellerProfile(principal.getName(), request);
            return ResponseEntity.ok("Seller profile updated successfully.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/update-buyer-profile")
    @PreAuthorize("hasRole('BUYER')") // Đảm bảo người dùng có vai trò BUYER
    public ResponseEntity<?> updateBuyerProfile(@RequestBody UpdateBuyerProfileRequest request, Principal principal) {
        try {
            userService.updateBuyerProfile(principal.getName(), request);
            return ResponseEntity.ok("Buyer profile updated successfully.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}