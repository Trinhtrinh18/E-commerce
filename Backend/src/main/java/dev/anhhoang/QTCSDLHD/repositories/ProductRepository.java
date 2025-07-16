package dev.anhhoang.QTCSDLHD.repositories;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.domain.Sort;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;
import org.springframework.data.mongodb.repository.Query;

import dev.anhhoang.QTCSDLHD.models.Product;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {
    List<Product> findByNameContainingIgnoreCase(String name);

    List<Product> findByShopid(String shopid);

    List<Product> findByNameContainingIgnoreCase(String name, Sort sort);

    List<Product> findByNameContainingIgnoreCaseAndCategory(String name, String category, Sort sort);

    List<Product> findByCategory(String category, Sort sort);

    @NonNull
    List<Product> findAll(@NonNull Sort sort);

    List<Product> findByNameContainingIgnoreCaseOrCategoryContainingIgnoreCase(String name, String category, Sort sort);

    @Query("{ 'category': ?0, '_id': { $ne: ?1 } }")
    List<Product> findByCategoryAndIdNot(String category, String id);
}