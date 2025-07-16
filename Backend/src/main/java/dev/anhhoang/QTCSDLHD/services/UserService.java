package dev.anhhoang.QTCSDLHD.services;

import dev.anhhoang.QTCSDLHD.dto.UserProfileResponse;
import dev.anhhoang.QTCSDLHD.dto.BecomeSellerRequest;
import dev.anhhoang.QTCSDLHD.models.*;
import dev.anhhoang.QTCSDLHD.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import dev.anhhoang.QTCSDLHD.dto.UpdateSellerProfileRequest;
import dev.anhhoang.QTCSDLHD.dto.UpdateBuyerProfileRequest;
import java.util.UUID;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public UserProfileResponse becomeSeller(String userEmail, BecomeSellerRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + userEmail));

        if (user.getRoles().contains(Role.ROLE_SELLER)) {
            throw new RuntimeException("User is already a seller.");
        }

        user.getRoles().add(Role.ROLE_SELLER);

        SellerProfile sellerProfile = new SellerProfile();
        sellerProfile.setShopId(UUID.randomUUID().toString());
        sellerProfile.setBusinessType(request.getBusinessType());
        sellerProfile.setShopName(request.getShopName());
        sellerProfile.setPhoneNumber(request.getPhoneNumber());
        sellerProfile.setPickupAddress(request.getPickupAddress());
        sellerProfile.setBankAccount(request.getBankAccount());
        sellerProfile.setShopLogoUrl(request.getShopLogoUrl());

        user.setSellerProfile(sellerProfile);

        // Lưu người dùng đã được cập nhật
        User updatedUser = userRepository.save(user);

        // Trả về DTO hồ sơ đã được cập nhật, thay vì chỉ trả về đối tượng User
        return findUserProfileByEmail(updatedUser.getEmail());
    }

    public UserProfileResponse findUserProfileByEmail(String email) {
        // 1. Find the user entity from the database
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        // 2. Create a new DTO object to hold the response data
        UserProfileResponse profileResponse = new UserProfileResponse();

        // 3. Manually map the data from the User entity to the DTO
        profileResponse.setId(user.getId());
        profileResponse.setEmail(user.getEmail());
        profileResponse.setFullName(user.getFullName());
        profileResponse.setRoles(user.getRoles());
        profileResponse.setBuyerProfile(user.getBuyerProfile());
        profileResponse.setSellerProfile(user.getSellerProfile());

        // 4. Return the DTO. The hashed password is never exposed.
        return profileResponse;
    }

    public User updateSellerProfile(String userEmail, UpdateSellerProfileRequest request) {
        // 1. Tìm người dùng bằng email
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + userEmail));

        // 2. Kiểm tra xem họ có phải là người bán không
        if (!user.getRoles().contains(Role.ROLE_SELLER) || user.getSellerProfile() == null) {
            throw new RuntimeException("User is not a seller. Cannot update seller profile.");
        }

        // 3. Lấy hồ sơ người bán hiện tại và cập nhật các trường
        SellerProfile currentProfile = user.getSellerProfile();
        currentProfile.setShopName(request.getShopName());
        currentProfile.setPhoneNumber(request.getPhoneNumber());
        currentProfile.setPickupAddress(request.getPickupAddress());
        currentProfile.setBankAccount(request.getBankAccount());

        // 4. Lưu lại toàn bộ đối tượng người dùng đã được cập nhật
        return userRepository.save(user);
    }

    public User updateBuyerProfile(String userEmail, UpdateBuyerProfileRequest request) {
        // 1. Tìm người dùng
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + userEmail));

        // 2. Lấy hoặc tạo mới hồ sơ người mua nếu chưa có
        BuyerProfile buyerProfile = user.getBuyerProfile();
        if (buyerProfile == null) {
            buyerProfile = new BuyerProfile();
        }

        // 3. Cập nhật thông tin
        buyerProfile.setPhoneNumber(request.getPhoneNumber());
        buyerProfile.setPrimaryAddress(request.getPrimaryAddress());
        buyerProfile.setBankAccount(request.getBankAccount());

        // 4. Gán lại hồ sơ đã cập nhật cho người dùng và lưu
        user.setBuyerProfile(buyerProfile);
        return userRepository.save(user);
    }
}