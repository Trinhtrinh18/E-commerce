package dev.anhhoang.QTCSDLHD.services;

import dev.anhhoang.QTCSDLHD.dto.ProductResponse;
import dev.anhhoang.QTCSDLHD.models.CartItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class CartCacheService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private ProductCacheService productCacheService;
    
    private static final String CART_ITEMS_KEY = "cart_items:";
    private static final long CACHE_EXPIRATION_HOURS = 12; // Cache expire sau 12 giờ
    
    /**
     * Cache sản phẩm khi thêm vào giỏ hàng
     */
    public void cacheProductWhenAddToCart(String customerId, String productId, int quantity) {
        // Cache thông tin sản phẩm
        ProductResponse product = productCacheService.getProductFromCache(productId);
        
        if (product != null) {
            // Cache item trong giỏ hàng với thông tin chi tiết
            String cartItemKey = CART_ITEMS_KEY + customerId + ":" + productId;
            CartItemCache cartItemCache = new CartItemCache();
            cartItemCache.setProductId(productId);
            cartItemCache.setQuantity(quantity);
            cartItemCache.setProduct(product);
            cartItemCache.setTimestamp(System.currentTimeMillis());
            
            redisTemplate.opsForValue().set(cartItemKey, cartItemCache, CACHE_EXPIRATION_HOURS, TimeUnit.HOURS);
            
            System.out.println("Cached product when adding to cart: " + productId + " for customer: " + customerId);
        }
    }
    
    /**
     * Lấy sản phẩm từ cache giỏ hàng
     */
    public CartItemCache getCartItemFromCache(String customerId, String productId) {
        String cartItemKey = CART_ITEMS_KEY + customerId + ":" + productId;
        
        try {
            Object cached = redisTemplate.opsForValue().get(cartItemKey);
            
            if (cached == null) {
                return null;
            }
            
            // Nếu cached là LinkedHashMap, convert sang CartItemCache
            if (cached instanceof java.util.LinkedHashMap) {
                @SuppressWarnings("unchecked")
                java.util.LinkedHashMap<String, Object> map = (java.util.LinkedHashMap<String, Object>) cached;
                CartItemCache cartItemCache = new CartItemCache();
                cartItemCache.setProductId((String) map.get("productId"));
                cartItemCache.setQuantity(((Number) map.get("quantity")).intValue());
                cartItemCache.setTimestamp(((Number) map.get("timestamp")).longValue());
                
                // Handle product object
                if (map.get("product") instanceof java.util.LinkedHashMap) {
                    @SuppressWarnings("unchecked")
                    java.util.LinkedHashMap<String, Object> productMap = (java.util.LinkedHashMap<String, Object>) map.get("product");
                    ProductResponse product = convertMapToProductResponse(productMap);
                    cartItemCache.setProduct(product);
                }
                
                return cartItemCache;
            }
            
            return (CartItemCache) cached;
        } catch (Exception e) {
            System.err.println("Error retrieving cart item from cache for key: " + cartItemKey + " - " + e.getMessage());
            // Xóa cache item bị lỗi
            redisTemplate.delete(cartItemKey);
            return null;
        }
    }
    
    /**
     * Helper method to convert LinkedHashMap to ProductResponse
     */
    private ProductResponse convertMapToProductResponse(java.util.LinkedHashMap<String, Object> map) {
        ProductResponse product = new ProductResponse();
        product.set_id((String) map.get("_id"));
        product.setName((String) map.get("name"));
        product.setDescription((String) map.get("description"));
        product.setPrice(map.get("price") != null ? ((Number) map.get("price")).doubleValue() : null);
        product.setStock(map.get("stock") != null ? ((Number) map.get("stock")).intValue() : null);
        product.setImage_url((String) map.get("image_url"));
        product.setCategory((String) map.get("category"));
        product.setShop_id((String) map.get("shop_id"));
        product.setShop_name((String) map.get("shop_name"));
        product.setQuantity(map.get("quantity") != null ? ((Number) map.get("quantity")).intValue() : null);
        product.setPurchaseCount(map.get("purchaseCount") != null ? ((Number) map.get("purchaseCount")).intValue() : null);
        product.setViewed(map.get("viewed") != null ? (Boolean) map.get("viewed") : null);
        product.setInteractionCount(map.get("interactionCount") != null ? ((Number) map.get("interactionCount")).intValue() : 0);
        return product;
    }
    
    /**
     * Lấy tất cả sản phẩm trong giỏ hàng từ cache
     */
    public List<CartItemCache> getAllCartItemsFromCache(String customerId) {
        String pattern = CART_ITEMS_KEY + customerId + ":*";
        return redisTemplate.keys(pattern).stream()
                .map(key -> {
                    try {
                        Object cached = redisTemplate.opsForValue().get(key);
                        if (cached == null) {
                            return null;
                        }
                        
                        // Nếu cached là LinkedHashMap, convert sang CartItemCache
                        if (cached instanceof java.util.LinkedHashMap) {
                            @SuppressWarnings("unchecked")
                            java.util.LinkedHashMap<String, Object> map = (java.util.LinkedHashMap<String, Object>) cached;
                            CartItemCache cartItemCache = new CartItemCache();
                            cartItemCache.setProductId((String) map.get("productId"));
                            cartItemCache.setQuantity(((Number) map.get("quantity")).intValue());
                            cartItemCache.setTimestamp(((Number) map.get("timestamp")).longValue());
                            
                            // Handle product object
                            if (map.get("product") instanceof java.util.LinkedHashMap) {
                                @SuppressWarnings("unchecked")
                                java.util.LinkedHashMap<String, Object> productMap = (java.util.LinkedHashMap<String, Object>) map.get("product");
                                ProductResponse product = convertMapToProductResponse(productMap);
                                cartItemCache.setProduct(product);
                            }
                            
                            return cartItemCache;
                        }
                        
                        return (CartItemCache) cached;
                    } catch (Exception e) {
                        System.err.println("Error processing cached cart item for key: " + key + " - " + e.getMessage());
                        // Xóa cache item bị lỗi
                        redisTemplate.delete(key);
                        return null;
                    }
                })
                .filter(item -> item != null)
                .collect(Collectors.toList());
    }
    
    /**
     * Cập nhật số lượng sản phẩm trong cache giỏ hàng
     */
    public void updateCartItemQuantity(String customerId, String productId, int quantity) {
        String cartItemKey = CART_ITEMS_KEY + customerId + ":" + productId;
        Object cached = redisTemplate.opsForValue().get(cartItemKey);
        
        if (cached != null) {
            CartItemCache cartItemCache;
            
            // Nếu cached là LinkedHashMap, convert sang CartItemCache
            if (cached instanceof java.util.LinkedHashMap) {
                @SuppressWarnings("unchecked")
                java.util.LinkedHashMap<String, Object> map = (java.util.LinkedHashMap<String, Object>) cached;
                cartItemCache = new CartItemCache();
                cartItemCache.setProductId((String) map.get("productId"));
                cartItemCache.setQuantity(quantity); // Set new quantity
                cartItemCache.setTimestamp(System.currentTimeMillis()); // Update timestamp
                
                // Handle product object
                if (map.get("product") instanceof java.util.LinkedHashMap) {
                    @SuppressWarnings("unchecked")
                    java.util.LinkedHashMap<String, Object> productMap = (java.util.LinkedHashMap<String, Object>) map.get("product");
                    ProductResponse product = convertMapToProductResponse(productMap);
                    cartItemCache.setProduct(product);
                }
            } else {
                cartItemCache = (CartItemCache) cached;
                cartItemCache.setQuantity(quantity);
                cartItemCache.setTimestamp(System.currentTimeMillis());
            }
            
            redisTemplate.opsForValue().set(cartItemKey, cartItemCache, CACHE_EXPIRATION_HOURS, TimeUnit.HOURS);
            
            System.out.println("Updated cart item quantity in cache: " + productId + " = " + quantity);
        }
    }
    
    /**
     * Xóa sản phẩm khỏi cache giỏ hàng
     */
    public void removeCartItemFromCache(String customerId, String productId) {
        String cartItemKey = CART_ITEMS_KEY + customerId + ":" + productId;
        redisTemplate.delete(cartItemKey);
        
        System.out.println("Removed cart item from cache: " + productId + " for customer: " + customerId);
    }
    
    /**
     * Xóa toàn bộ cache giỏ hàng của khách hàng
     */
    public void clearCartCache(String customerId) {
        String pattern = CART_ITEMS_KEY + customerId + ":*";
        redisTemplate.delete(redisTemplate.keys(pattern));
        
        System.out.println("Cleared cart cache for customer: " + customerId);
    }
    
    /**
     * Kiểm tra xem sản phẩm có trong cache giỏ hàng không
     */
    public boolean isProductInCartCache(String customerId, String productId) {
        String cartItemKey = CART_ITEMS_KEY + customerId + ":" + productId;
        return redisTemplate.hasKey(cartItemKey);
    }
    
    /**
     * Đồng bộ cache với database (có thể gọi định kỳ)
     */
    public void syncCacheWithDatabase(String customerId, List<CartItem> cartItems) {
        // Xóa cache cũ
        clearCartCache(customerId);
        
        // Cache lại các items mới
        for (CartItem item : cartItems) {
            cacheProductWhenAddToCart(customerId, item.getProduct_id(), item.getQuantity());
        }
    }
    
    /**
     * Xóa cache cho các sản phẩm đã thanh toán
     */
    public void removeOrderedItemsFromCache(String customerId, List<String> productIds) {
        for (String productId : productIds) {
            String cartItemKey = CART_ITEMS_KEY + customerId + ":" + productId;
            redisTemplate.delete(cartItemKey);
            System.out.println("Removed ordered item from cache: " + productId + " for customer: " + customerId);
        }
    }
    
    /**
     * Cập nhật số lượng tồn kho của sản phẩm trong cache giỏ hàng
     * Được gọi sau khi stock sản phẩm thay đổi (sau khi mua hàng)
     */
    public void updateProductStockInCache(String productId, int newStock) {
        try {
            // Lấy tất cả keys có pattern cart_items:*:{productId}
            String pattern = CART_ITEMS_KEY + "*:" + productId;
            var keys = redisTemplate.keys(pattern);
            
            if (keys != null && !keys.isEmpty()) {
                for (String key : keys) {
                    Object cached = redisTemplate.opsForValue().get(key);
                    if (cached != null) {
                        CartItemCache cartItemCache = null;
                        
                        // Xử lý trường hợp cached là LinkedHashMap
                        if (cached instanceof java.util.LinkedHashMap) {
                            @SuppressWarnings("unchecked")
                            java.util.LinkedHashMap<String, Object> map = (java.util.LinkedHashMap<String, Object>) cached;
                            cartItemCache = convertMapToCartItemCache(map);
                        } else if (cached instanceof CartItemCache) {
                            cartItemCache = (CartItemCache) cached;
                        }
                        
                        if (cartItemCache != null && cartItemCache.getProduct() != null) {
                            // Cập nhật stock mới
                            cartItemCache.getProduct().setStock(newStock);
                            cartItemCache.setTimestamp(System.currentTimeMillis());
                            
                            // Lưu lại vào cache
                            redisTemplate.opsForValue().set(key, cartItemCache, CACHE_EXPIRATION_HOURS, TimeUnit.HOURS);
                            System.out.println("Updated stock for product " + productId + " in cache: " + newStock);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error updating product stock in cache: " + e.getMessage());
        }
    }
    
    /**
     * Convert LinkedHashMap to CartItemCache
     */
    private CartItemCache convertMapToCartItemCache(java.util.LinkedHashMap<String, Object> map) {
        CartItemCache cartItemCache = new CartItemCache();
        cartItemCache.setProductId((String) map.get("productId"));
        cartItemCache.setQuantity(((Number) map.get("quantity")).intValue());
        cartItemCache.setTimestamp(((Number) map.get("timestamp")).longValue());
        
        // Convert product object
        if (map.get("product") instanceof java.util.LinkedHashMap) {
            @SuppressWarnings("unchecked")
            java.util.LinkedHashMap<String, Object> productMap = (java.util.LinkedHashMap<String, Object>) map.get("product");
            ProductResponse product = convertMapToProductResponse(productMap);
            cartItemCache.setProduct(product);
        }
        
        return cartItemCache;
    }

    /**
     * Inner class để lưu thông tin cache của cart item
     */
    public static class CartItemCache {
        private String productId;
        private int quantity;
        private ProductResponse product;
        private long timestamp;
        
        // Getters and setters
        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }
        
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        
        public ProductResponse getProduct() { return product; }
        public void setProduct(ProductResponse product) { this.product = product; }
        
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }
}
