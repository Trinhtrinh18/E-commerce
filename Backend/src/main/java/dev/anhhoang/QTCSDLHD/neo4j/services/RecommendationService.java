package dev.anhhoang.QTCSDLHD.neo4j.services;

import java.util.List;
import dev.anhhoang.QTCSDLHD.dto.ProductResponse;

public interface RecommendationService {
    List<String> getRecommendedProductIdsForUser(String userId);

    void recordProductView(String userId, String productId);

    void recordProductPurchase(String userId, String productId, int quantity);

    List<ProductResponse> getRecommendedProductsForUser(String userId);

    List<ProductResponse> getSimilarProductsForProduct(String productId, String userId);
}