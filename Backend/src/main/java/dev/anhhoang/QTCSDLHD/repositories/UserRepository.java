// src/main/java/dev/anhhoang/QTCSDLHD/repositories/UserRepository.java
package dev.anhhoang.QTCSDLHD.repositories;

import dev.anhhoang.QTCSDLHD.models.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    // We need a way to find a user by their email for login and to check if they
    // already exist
    Optional<User> findByEmail(String email);

    // Find a user by their seller profile's shopId
    Optional<User> findBySellerProfileShopId(String shopId);
}