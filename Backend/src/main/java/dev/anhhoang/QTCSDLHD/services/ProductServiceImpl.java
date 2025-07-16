package dev.anhhoang.QTCSDLHD.services;

import dev.anhhoang.QTCSDLHD.dto.ProductResponse;
import dev.anhhoang.QTCSDLHD.models.Product;
import dev.anhhoang.QTCSDLHD.repositories.ProductRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private Neo4jClient neo4jClient;

    @Override
    public List<ProductResponse> getAllProducts(String sort) {
        Sort sortOrder = createSortOrder(sort);
        List<Product> products = productRepository.findAll(sortOrder);
        return convertToDtoWithPurchaseCount(products);
    }

    @Override
    public ProductResponse getProductById(String id) {
        Optional<Product> productOpt = productRepository.findById(id);
        Product product = productOpt.orElseThrow(() -> new RuntimeException("Product not found"));
        return convertToDtoWithPurchaseCount(List.of(product)).get(0);
    }

    @Override
    public List<ProductResponse> searchProducts(String keyword, String category, String sort) {
        Sort sortOrder = createSortOrder(sort);
        List<Product> products;

        if (keyword != null && !keyword.isEmpty() && category != null && !category.isEmpty()) {
            products = productRepository.findByNameContainingIgnoreCaseAndCategory(keyword, category, sortOrder);
        } else if (keyword != null && !keyword.isEmpty()) {
            products = productRepository.findByNameContainingIgnoreCaseOrCategoryContainingIgnoreCase(keyword, keyword,
                    sortOrder);
        } else if (category != null && !category.isEmpty()) {
            products = productRepository.findByCategory(category, sortOrder);
        } else {
            products = productRepository.findAll(sortOrder);
        }

        return convertToDtoWithPurchaseCount(products);
    }

    @Override
    public List<String> getAllCategories() {
        return productRepository.findAll().stream()
                .map(Product::getCategory)
                .distinct()
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<ProductResponse> convertToDtoWithPurchaseCount(List<Product> products) {
        if (products.isEmpty()) {
            return List.of();
        }

        List<String> productIds = products.stream().map(Product::get_id).collect(Collectors.toList());

        String purchaseCountCypher = """
                    UNWIND $ids AS pid
                    MATCH (p:Product {id: pid})
                    OPTIONAL MATCH (u:User)-[b:BOUGHT]->(p)
                    RETURN pid, COALESCE(SUM(b.quantity), 0) AS purchaseCount
                """;

        Collection<Map<String, Object>> purchaseCountsResult = neo4jClient.query(purchaseCountCypher)
                .bindAll(Map.of("ids", productIds))
                .fetch().all();

        Map<String, Integer> purchaseCountMap = new HashMap<>();
        for (var row : purchaseCountsResult) {
            purchaseCountMap.put((String) row.get("pid"), ((Number) row.get("purchaseCount")).intValue());
        }

        return products.stream().map(product -> {
            ProductResponse dto = new ProductResponse();
            BeanUtils.copyProperties(product, dto);
            dto.setShop_name(product.getShopname());
            dto.setPurchaseCount(purchaseCountMap.getOrDefault(product.get_id(), 0));
            // Note: viewed status is not available here, it's specific to a user context
            dto.setViewed(false);
            return dto;
        }).collect(Collectors.toList());
    }

    private Sort createSortOrder(String sort) {
        if (sort == null || sort.isEmpty()) {
            return Sort.unsorted();
        }
        switch (sort) {
            case "priceAsc":
                return Sort.by(Sort.Direction.ASC, "price");
            case "priceDesc":
                return Sort.by(Sort.Direction.DESC, "price");
            case "newest":
                return Sort.by(Sort.Direction.DESC, "created_at");
            case "oldest":
                return Sort.by(Sort.Direction.ASC, "created_at");
            default:
                return Sort.unsorted();
        }
    }
}