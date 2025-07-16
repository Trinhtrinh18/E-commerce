package dev.anhhoang.QTCSDLHD.neo4j.services;

import dev.anhhoang.QTCSDLHD.neo4j.entities.UserNode;
import dev.anhhoang.QTCSDLHD.neo4j.entities.ProductNode;
import dev.anhhoang.QTCSDLHD.neo4j.repositories.UserNodeRepository;
import dev.anhhoang.QTCSDLHD.neo4j.repositories.ProductNodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import dev.anhhoang.QTCSDLHD.dto.ProductResponse;
import dev.anhhoang.QTCSDLHD.models.Product;
import dev.anhhoang.QTCSDLHD.repositories.ProductRepository;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Set;
import java.util.HashSet;
import java.util.Comparator;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

@Service
public class RecommendationServiceImpl implements RecommendationService {
    @Autowired
    private UserNodeRepository userNodeRepository;
    @Autowired
    private ProductNodeRepository productNodeRepository;
    @Autowired
    private Neo4jClient neo4jClient;
    @Autowired
    private ProductRepository productRepository;

    @Override
    @Transactional
    public void recordProductView(String userId, String productId) {
        neo4jClient.query(
                "MERGE (u:User {id: $userId}) MERGE (p:Product {id: $productId}) MERGE (u)-[v:VIEWED]->(p) ON CREATE SET v.timestamp = datetime() ON MATCH SET v.timestamp = datetime()")
                .bind(userId).to("userId")
                .bind(productId).to("productId")
                .run();
    }

    @Override
    @Transactional
    public void recordProductPurchase(String userId, String productId, int quantity) {
        neo4jClient.query(
                "MERGE (u:User {id: $userId}) MERGE (p:Product {id: $productId}) MERGE (u)-[b:BOUGHT]->(p) ON CREATE SET b.quantity = $quantity, b.timestamp = datetime() ON MATCH SET b.quantity = b.quantity + $quantity, b.timestamp = datetime()")
                .bind(userId).to("userId")
                .bind(productId).to("productId")
                .bind(quantity).to("quantity")
                .run();
    }

    @Override
    public List<String> getRecommendedProductIdsForUser(String userId) {
        // Deprecated: not used, but required for interface
        throw new UnsupportedOperationException("Use getRecommendedProductsForUser instead");
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getRecommendedProductsForUser(String userId) {
        // Map để lưu trữ ID sản phẩm và điểm số kết hợp của nó (để sắp xếp)
        Map<String, Integer> productScores = new HashMap<>();
        // Set để theo dõi các ID sản phẩm đã được thêm vào, đảm bảo tính duy nhất
        Set<String> uniqueProductIdsTracker = new HashSet<>();

        // Define scoring constants
        final int SIMILAR_PRODUCT_BASE_SCORE = 1000000; // High score for similar products
        final int COLLABORATIVE_PRODUCT_BASE_SCORE = 500000; // Medium score for collaborative products

        // 1. Lấy sản phẩm user đã xem gần đây nhất (sản phẩm hiện tại đang xem)
        String recentViewedCypher = """
                    MATCH (u:User {id: $userId})-[v:VIEWED]->(p:Product)
                    RETURN p.id AS pid, v.timestamp AS timestamp
                    ORDER BY v.timestamp DESC
                    LIMIT 1
                """;

        List<String> recentViewedIds = neo4jClient.query(recentViewedCypher)
                .bind(userId).to("userId")
                .fetch().all().stream()
                .map(m -> (String) m.get("pid"))
                .collect(Collectors.toList());

        System.out.println("Recent Viewed IDs: " + recentViewedIds); // Log 1

        if (!recentViewedIds.isEmpty()) {
            String currentProductId = recentViewedIds.get(0);

            // 2. Lấy thông tin sản phẩm hiện tại từ MongoDB
            Product currentProduct = productRepository.findById(currentProductId).orElse(null);

            if (currentProduct != null) {
                // 3. Tìm sản phẩm tương tự dựa trên category và price range
                List<Product> similarProducts = productRepository.findByCategoryAndIdNot(
                        currentProduct.getCategory(),
                        currentProductId);

                // Add similar products with a very high score to ensure they are at the top
                for (Product p : similarProducts) {
                    if (uniqueProductIdsTracker.add(p.get_id())) {
                        // Fetch interaction count for similar product
                        String interactionCountCypher = """
                                    MATCH (p:Product {id: $pid})
                                    OPTIONAL MATCH (p)<-[r:VIEWED|BOUGHT]-()
                                    RETURN COUNT(r) AS interactionCount
                                """;
                        Integer interactionCount = neo4jClient.query(interactionCountCypher)
                                .bind(p.get_id()).to("pid")
                                .fetchAs(Integer.class)
                                .one().orElse(0);
                        productScores.put(p.get_id(), SIMILAR_PRODUCT_BASE_SCORE + interactionCount);
                    }
                }
            }
        }

        System.out.println("Product Scores after Similar Products: " + productScores); // Log 2

        // 4. Lấy gợi ý dựa trên hành vi user (collaborative filtering)
        // Only fetch if not already enough unique products collected (optional
        // optimization)
        if (uniqueProductIdsTracker.size() < 40) { // Fetch enough collaborative to potentially fill up to 40 slots
            String collaborativeCypher = """
                        MATCH (u:User {id: $userId})
                        OPTIONAL MATCH (u)-[:VIEWED]->(p1:Product)
                        OPTIONAL MATCH (u)-[:BOUGHT]->(p2:Product)
                        WITH COLLECT(DISTINCT p1.id) + COLLECT(DISTINCT p2.id) AS userProducts
                        UNWIND userProducts AS userProductId
                        WITH userProductId WHERE userProductId IS NOT NULL
                        // Tìm những user khác đã xem/mua cùng sản phẩm
                        MATCH (u2:User)-[:VIEWED|BOUGHT]->(p:Product {id: userProductId})
                        WHERE u2.id <> $userId
                        // Tìm sản phẩm khác mà những user này đã xem/mua
                        MATCH (u2)-[:VIEWED|BOUGHT]->(p3:Product)
                        WHERE p3.id <> userProductId
                        RETURN DISTINCT p3.id AS pid
                        LIMIT 25
                    """;

            List<String> collaborativeIds = neo4jClient.query(collaborativeCypher)
                    .bind(userId).to("userId")
                    .fetch().all().stream()
                    .map(m -> (String) m.get("pid"))
                    .collect(Collectors.toList());

            for (String id : collaborativeIds) {
                if (uniqueProductIdsTracker.add(id)) {
                    // Fetch interaction count for collaborative product
                    String interactionCountCypher = """
                                MATCH (p:Product {id: $pid})
                                OPTIONAL MATCH (p)<-[r:VIEWED|BOUGHT]-()
                                RETURN COUNT(r) AS interactionCount
                            """;
                    Integer interactionCount = neo4jClient.query(interactionCountCypher)
                            .bind(id).to("pid")
                            .fetchAs(Integer.class)
                            .one().orElse(0);
                    productScores.put(id, COLLABORATIVE_PRODUCT_BASE_SCORE + interactionCount);
                }
            }
        }

        System.out.println("Product Scores after Collaborative Filtering: " + productScores); // Log 3

        // 5. Lấy sản phẩm trending (lấp đầy nếu vẫn chưa đủ sản phẩm)
        // Always fetch trending to ensure a baseline set of recommendations
        if (uniqueProductIdsTracker.size() < 40) { // If total unique products are still less than max limit
            String trendingCypher = """
                        MATCH (p:Product)<-[r:VIEWED|BOUGHT]-()
                        RETURN p.id AS pid, COUNT(r) AS interactions
                        ORDER BY interactions DESC
                        LIMIT 40
                    """;
            Collection<Map<String, Object>> trendingResults = neo4jClient.query(trendingCypher)
                    .fetch().all();

            System.out.println("Trending Products from Neo4j (ORDERED): " + trendingResults); // Log 4

            for (Map<String, Object> result : trendingResults) {
                String id = (String) result.get("pid");
                Integer interactions = ((Number) result.get("interactions")).intValue();
                if (uniqueProductIdsTracker.add(id)) {
                    productScores.put(id, interactions); // Trending products use raw interaction count as score
                }
            }
        }

        // Convert map keys to list for final processing
        List<String> finalRecommendedIdsBasedOnScores = new ArrayList<>(productScores.keySet());

        if (finalRecommendedIdsBasedOnScores.isEmpty()) {
            return new java.util.ArrayList<>(); // No recommendations found
        }

        // Sort by the combined score (interactionCount field in DTO will store this
        // score) in descending order
        // Lấy danh sách productId đã xem của user (để hiển thị isViewed)
        String viewedCypher = """
                    MATCH (u:User {id: $userId})-[v:VIEWED]->(p:Product)
                    RETURN p.id AS pid
                """;
        Set<String> viewedIds = new HashSet<>(neo4jClient.query(viewedCypher)
                .bind(userId).to("userId")
                .fetch().all().stream()
                .map(m -> (String) m.get("pid"))
                .collect(Collectors.toList()));

        // Lấy purchaseCount cho từng productId (tổng quantity)
        String purchaseCountCypher = """
                    UNWIND $ids AS pid
                    MATCH (p:Product {id: pid})
                    OPTIONAL MATCH (u:User)-[b:BOUGHT]->(p)
                    RETURN pid, COALESCE(SUM(b.quantity), 0) AS purchaseCount
                """;
        Map<String, Integer> purchaseCountMap = new HashMap<>();
        neo4jClient.query(purchaseCountCypher)
                .bindAll(Map.of("ids", finalRecommendedIdsBasedOnScores))
                .fetch().all().forEach(row -> {
                    purchaseCountMap.put((String) row.get("pid"), ((Number) row.get("purchaseCount")).intValue());
                });

        // Lấy thông tin sản phẩm từ MongoDB
        List<Product> products = productRepository.findAllById(finalRecommendedIdsBasedOnScores);
        Map<String, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::get_id, p -> p));

        // Tạo ProductResponse cho từng id và sắp xếp dựa trên điểm số kết hợp
        List<ProductResponse> responses = finalRecommendedIdsBasedOnScores.stream()
                .map(pid -> {
                    Product p = productMap.get(pid);
                    if (p == null)
                        return null;
                    ProductResponse dto = new ProductResponse();
                    org.springframework.beans.BeanUtils.copyProperties(p, dto);
                    dto.setShop_name(p.getShopname());
                    dto.setPurchaseCount(purchaseCountMap.getOrDefault(pid, 0));
                    dto.setViewed(viewedIds.contains(pid));
                    dto.setInteractionCount(productScores.getOrDefault(pid, 0)); // Store the combined score here
                    return dto;
                })
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparingInt(ProductResponse::getInteractionCount).reversed()) // Sort by combined
                                                                                                  // score
                .limit(40) // Apply final limit after sorting
                .collect(Collectors.toList());

        System.out.println("Final Recommended IDs after DTO conversion and sorting: "
                + responses.stream().map(ProductResponse::get_id).collect(Collectors.toList())); // Log 5

        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getSimilarProductsForProduct(String productId, String userId) {
        // 1. Lấy thông tin sản phẩm hiện tại từ MongoDB
        Product currentProduct = productRepository.findById(productId).orElse(null);

        if (currentProduct == null) {
            return new java.util.ArrayList<>();
        }

        // 2. Tìm sản phẩm tương tự dựa trên category và price range
        List<Product> similarProducts = productRepository.findByCategoryAndIdNot(
                currentProduct.getCategory(),
                productId);

        // 3. Lấy thông tin tần suất xem/mua từ Neo4j
        String interactionCountCypher = """
                    UNWIND $productIds AS pid
                    MATCH (p:Product {id: pid})
                    OPTIONAL MATCH (p)<-[r:VIEWED|BOUGHT]-()
                    RETURN pid, COUNT(r) AS interactionCount
                """;

        List<String> productIds = similarProducts.stream()
                .map(Product::get_id)
                .collect(Collectors.toList());

        if (productIds.isEmpty()) {
            return new java.util.ArrayList<>();
        }

        Collection<java.util.Map<String, Object>> interactionCounts = neo4jClient.query(interactionCountCypher)
                .bindAll(java.util.Map.of("productIds", productIds))
                .fetch().all();

        java.util.Map<String, Integer> interactionCountMap = new java.util.HashMap<>();
        for (var row : interactionCounts) {
            interactionCountMap.put((String) row.get("pid"), ((Number) row.get("interactionCount")).intValue());
        }

        // 4. Tính điểm tương đồng và sắp xếp
        List<Product> scoredProducts = similarProducts.stream()
                .map(product -> {
                    double priceDiff = Math.abs(product.getPrice() - currentProduct.getPrice());
                    double priceRatio = priceDiff / currentProduct.getPrice();

                    int priceScore;
                    if (priceRatio <= 0.3)
                        priceScore = 3;
                    else if (priceRatio <= 0.5)
                        priceScore = 2;
                    else
                        priceScore = 1;

                    // Điểm dựa trên tần suất xem/mua
                    int interactionCount = interactionCountMap.getOrDefault(product.get_id(), 0);
                    int interactionScore = Math.min(interactionCount, 10); // Giới hạn tối đa 10 điểm

                    // Điểm tổng hợp: ưu tiên price similarity hơn interaction
                    int totalScore = priceScore * 2 + interactionScore;

                    return new java.util.AbstractMap.SimpleEntry<>(product, totalScore);
                })
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(40)
                .map(java.util.Map.Entry::getKey)
                .collect(Collectors.toList());

        // 5. Lấy danh sách productId đã xem của user
        String viewedCypher = """
                    MATCH (u:User {id: $userId})-[v:VIEWED]->(p:Product)
                    RETURN p.id AS pid
                """;
        List<String> viewedIds = neo4jClient.query(viewedCypher)
                .bind(userId).to("userId")
                .fetch().all().stream()
                .map(m -> (String) m.get("pid"))
                .collect(Collectors.toList());

        // 6. Lấy purchaseCount cho từng productId
        String purchaseCountCypher = """
                    UNWIND $ids AS pid
                    MATCH (p:Product {id: pid})
                    OPTIONAL MATCH (u:User)-[b:BOUGHT]->(p)
                    RETURN pid, COALESCE(SUM(b.quantity), 0) AS purchaseCount
                """;

        List<String> recommendedIds = scoredProducts.stream()
                .map(Product::get_id)
                .collect(Collectors.toList());

        Collection<java.util.Map<String, Object>> purchaseCounts = neo4jClient.query(purchaseCountCypher)
                .bindAll(java.util.Map.of("ids", recommendedIds))
                .fetch().all();
        java.util.Map<String, Integer> purchaseCountMap = new java.util.HashMap<>();
        for (var row : purchaseCounts) {
            purchaseCountMap.put((String) row.get("pid"), ((Number) row.get("purchaseCount")).intValue());
        }

        // 7. Tạo ProductResponse cho từng id
        List<ProductResponse> responses = recommendedIds.stream()
                .map(pid -> {
                    Product p = scoredProducts.stream()
                            .filter(product -> product.get_id().equals(pid))
                            .findFirst()
                            .orElse(null);
                    if (p == null)
                        return null;
                    ProductResponse dto = new ProductResponse();
                    org.springframework.beans.BeanUtils.copyProperties(p, dto);
                    dto.setShop_name(p.getShopname());
                    dto.setPurchaseCount(purchaseCountMap.getOrDefault(pid, 0).intValue());
                    dto.setViewed(viewedIds.contains(pid));
                    return dto;
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
        return responses;
    }

    private ProductResponse toProductResponse(Product product, List<String> viewedIds,
            java.util.Map<String, Long> purchaseCountMap) {
        ProductResponse dto = new ProductResponse();
        org.springframework.beans.BeanUtils.copyProperties(product, dto);
        dto.setShop_name(product.getShopname());
        dto.setPurchaseCount(purchaseCountMap.getOrDefault(product.get_id(), 0L).intValue());
        dto.setViewed(viewedIds.contains(product.get_id()));
        return dto;
    }
}