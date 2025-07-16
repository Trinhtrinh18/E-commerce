package dev.anhhoang.QTCSDLHD.repositories;

import dev.anhhoang.QTCSDLHD.models.Cart;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends MongoRepository<Cart, String> {
    Optional<Cart> findByCustomerId(String customerId);

    void deleteByCustomerId(String customerId);
}