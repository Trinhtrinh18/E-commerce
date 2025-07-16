package dev.anhhoang.QTCSDLHD.repositories;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import dev.anhhoang.QTCSDLHD.models.Order;

@Repository
public interface OrderRepository extends MongoRepository<Order, String> {
    @Query("{ 'customer_id': ?0 }")
    List<Order> findByCustomer_id(String customerId);
}