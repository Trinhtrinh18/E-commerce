package dev.anhhoang.QTCSDLHD.services;

import dev.anhhoang.QTCSDLHD.dto.ProductResponse;
import dev.anhhoang.QTCSDLHD.models.Product;
import dev.anhhoang.QTCSDLHD.repositories.ProductRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.Optional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class ProductCacheService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private ProductRepository productRepository;
    
    private static final String PRODUCT_CACHE_KEY = "product:";
    private static final long CACHE_EXPIRATION_HOURS = 24; // Cache expire sau 24 giờ
    
    /**
     * Lấy sản phẩm từ cache, nếu không có thì lấy từ database và cache lại
     */
    public ProductResponse getProductFromCache(String productId) {
        String cacheKey = PRODUCT_CACHE_KEY + productId;
        
        try {
            // Tìm trong cache trước
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            
            if (cached != null) {
                ProductResponse cachedProduct = null;
                
                // Nếu cached là LinkedHashMap, convert sang ProductResponse
                if (cached instanceof java.util.LinkedHashMap) {
                    @SuppressWarnings("unchecked")
                    java.util.LinkedHashMap<String, Object> map = (java.util.LinkedHashMap<String, Object>) cached;
                    cachedProduct = convertMapToProductResponse(map);
                } else {
                    cachedProduct = (ProductResponse) cached;
                }
                
                if (cachedProduct != null) {
                    System.out.println("Product found in cache: " + productId);
                    return cachedProduct;
                }
            }
        } catch (Exception e) {
            System.err.println("Error retrieving product from cache for key: " + cacheKey + " - " + e.getMessage());
            // Xóa cache item bị lỗi
            redisTemplate.delete(cacheKey);
        }
        
        // Nếu không có trong cache hoặc có lỗi, lấy từ database
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isPresent()) {
            Product product = productOpt.get();
            ProductResponse productResponse = new ProductResponse();
            BeanUtils.copyProperties(product, productResponse);
            
            // Cache sản phẩm
            cacheProduct(productResponse);
            System.out.println("Product cached from database: " + productId);
            return productResponse;
        }
        
        return null;
    }
    
    /**
     * Convert LinkedHashMap to ProductResponse
     */
    private ProductResponse convertMapToProductResponse(java.util.LinkedHashMap<String, Object> map) {
        ProductResponse product = new ProductResponse();
        
        try {
            product.set_id((String) map.get("_id"));
            product.setName((String) map.get("name"));
            product.setDescription((String) map.get("description"));
            product.setPrice(((Number) map.get("price")).doubleValue());
            product.setQuantity(((Number) map.get("quantity")).intValue());
            product.setImage_url((String) map.get("image_url"));
            product.setCategory((String) map.get("category"));
            product.setShop_id((String) map.get("shop_id"));
            product.setShop_name((String) map.get("shop_name"));
            
            // Handle stock field
            if (map.get("stock") != null) {
                product.setStock(((Number) map.get("stock")).intValue());
            }
            
            // Handle purchaseCount field
            if (map.get("purchaseCount") != null) {
                product.setPurchaseCount(((Number) map.get("purchaseCount")).intValue());
            }
            
            // Handle viewed field
            if (map.get("viewed") != null) {
                product.setViewed((Boolean) map.get("viewed"));
            }
            
            // Handle LocalDateTime fields
            if (map.get("created_at") instanceof String) {
                product.setCreated_at(parseLocalDateTime((String) map.get("created_at")));
            }
            if (map.get("updated_at") instanceof String) {
                product.setUpdated_at(parseLocalDateTime((String) map.get("updated_at")));
            }
            
            return product;
        } catch (Exception e) {
            System.err.println("Error converting LinkedHashMap to ProductResponse: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Parse LocalDateTime from String
     */
    private LocalDateTime parseLocalDateTime(String dateTimeStr) {
        try {
            if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
                return null;
            }
            // Handle various date formats
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
            return LocalDateTime.parse(dateTimeStr, formatter);
        } catch (Exception e) {
            System.err.println("Error parsing LocalDateTime: " + dateTimeStr + " - " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Cache sản phẩm vào Redis
     */
    public void cacheProduct(ProductResponse product) {
        String cacheKey = PRODUCT_CACHE_KEY + product.get_id();
        redisTemplate.opsForValue().set(cacheKey, product, CACHE_EXPIRATION_HOURS, TimeUnit.HOURS);
    }
    
    /**
     * Xóa sản phẩm khỏi cache (khi sản phẩm được cập nhật)
     */
    public void evictProductFromCache(String productId) {
        String cacheKey = PRODUCT_CACHE_KEY + productId;
        redisTemplate.delete(cacheKey);
        System.out.println("Product evicted from cache: " + productId);
    }
    
    /**
     * Cập nhật thông tin sản phẩm trong cache
     */
    public void updateProductInCache(String productId, ProductResponse product) {
        evictProductFromCache(productId);
        cacheProduct(product);
    }
    
    /**
     * Kiểm tra xem sản phẩm có tồn tại trong cache không
     */
    public boolean isProductInCache(String productId) {
        String cacheKey = PRODUCT_CACHE_KEY + productId;
        return redisTemplate.hasKey(cacheKey);
    }
    
    /**
     * Cập nhật stock của sản phẩm trong cache
     */
    public void updateProductStockInCache(String productId, int newStock) {
        String cacheKey = PRODUCT_CACHE_KEY + productId;
        
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            
            if (cached != null) {
                ProductResponse product = null;
                
                // Nếu cached là LinkedHashMap, convert sang ProductResponse
                if (cached instanceof java.util.LinkedHashMap) {
                    @SuppressWarnings("unchecked")
                    java.util.LinkedHashMap<String, Object> map = (java.util.LinkedHashMap<String, Object>) cached;
                    product = convertMapToProductResponse(map);
                } else {
                    product = (ProductResponse) cached;
                }
                
                if (product != null) {
                    // Cập nhật stock mới
                    product.setStock(newStock);
                    
                    // Lưu lại vào cache
                    redisTemplate.opsForValue().set(cacheKey, product, CACHE_EXPIRATION_HOURS, TimeUnit.HOURS);
                    System.out.println("Updated stock for product " + productId + " in product cache: " + newStock);
                }
            }
        } catch (Exception e) {
            System.err.println("Error updating product stock in cache: " + e.getMessage());
        }
    }
}
