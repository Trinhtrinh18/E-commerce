package dev.anhhoang.QTCSDLHD.controllers;

import dev.anhhoang.QTCSDLHD.services.CartCacheService;
import dev.anhhoang.QTCSDLHD.services.ProductCacheService;
import dev.anhhoang.QTCSDLHD.services.CacheMonitoringService;
import dev.anhhoang.QTCSDLHD.services.UserService;
import dev.anhhoang.QTCSDLHD.dto.UserProfileResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/cache")
public class CacheController {

    @Autowired
    private CartCacheService cartCacheService;

    @Autowired
    private ProductCacheService productCacheService;

    @Autowired
    private CacheMonitoringService cacheMonitoringService;

    @Autowired
    private UserService userService;

    private String getCustomerId(Principal principal) {
        if (principal == null) {
            return null;
        }
        UserProfileResponse userProfile = userService.findUserProfileByEmail(principal.getName());
        if (userProfile == null || userProfile.getId() == null) {
            throw new RuntimeException("User profile not found or ID missing.");
        }
        return userProfile.getId();
    }

    /**
     * Public endpoint - Kiểm tra kết nối Redis (không cần authentication)
     */
    @GetMapping("/health")
    public ResponseEntity<?> checkRedisHealth() {
        try {
            boolean connected = cacheMonitoringService.isRedisConnected();
            return ResponseEntity.ok(java.util.Map.of(
                "redisConnected", connected,
                "status", connected ? "healthy" : "unhealthy",
                "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error checking Redis health: " + e.getMessage());
        }
    }

    /**
     * Public endpoint - Lấy thống kê cache tổng quan (không cần authentication)
     */
    @GetMapping("/statistics")
    public ResponseEntity<?> getCacheStatistics() {
        try {
            CacheMonitoringService.CacheStatistics stats = cacheMonitoringService.getCacheStatistics();
            return ResponseEntity.ok(java.util.Map.of(
                "statistics", stats,
                "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error getting cache statistics: " + e.getMessage());
        }
    }

    /**
     * Lấy tất cả sản phẩm trong cache giỏ hàng
     */
    @GetMapping("/cart-items")
    public ResponseEntity<?> getCachedCartItems(Principal principal) {
        try {
            String customerId = getCustomerId(principal);
            List<CartCacheService.CartItemCache> cachedItems = cartCacheService.getAllCartItemsFromCache(customerId);
            return ResponseEntity.ok(cachedItems);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error getting cached cart items: " + e.getMessage());
        }
    }

    /**
     * Kiểm tra sản phẩm có trong cache không
     */
    @GetMapping("/product/{productId}/exists")
    public ResponseEntity<?> checkProductInCache(@PathVariable String productId) {
        try {
            boolean exists = productCacheService.isProductInCache(productId);
            return ResponseEntity.ok(exists);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error checking product in cache: " + e.getMessage());
        }
    }

    /**
     * Xóa cache giỏ hàng của khách hàng
     */
    @DeleteMapping("/cart/clear")
    public ResponseEntity<?> clearCartCache(Principal principal) {
        try {
            String customerId = getCustomerId(principal);
            cartCacheService.clearCartCache(customerId);
            return ResponseEntity.ok("Cart cache cleared successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error clearing cart cache: " + e.getMessage());
        }
    }

    /**
     * Xóa sản phẩm khỏi cache
     */
    @DeleteMapping("/product/{productId}")
    public ResponseEntity<?> evictProductFromCache(@PathVariable String productId) {
        try {
            productCacheService.evictProductFromCache(productId);
            return ResponseEntity.ok("Product evicted from cache successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error evicting product from cache: " + e.getMessage());
        }
    }

    /**
     * Lấy thông tin cache status
     */
    @GetMapping("/status")
    public ResponseEntity<?> getCacheStatus(Principal principal) {
        try {
            String customerId = getCustomerId(principal);
            List<CartCacheService.CartItemCache> cachedItems = cartCacheService.getAllCartItemsFromCache(customerId);
            
            return ResponseEntity.ok(java.util.Map.of(
                "customerId", customerId,
                "cachedItemsCount", cachedItems.size(),
                "cachedItems", cachedItems
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error getting cache status: " + e.getMessage());
        }
    }

    /**
     * Xóa tất cả cache (Public endpoint - chỉ dùng cho testing)
     */
    @DeleteMapping("/clear-all")
    public ResponseEntity<?> clearAllCache() {
        try {
            cacheMonitoringService.clearAllCache();
            return ResponseEntity.ok("All cache cleared successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error clearing all cache: " + e.getMessage());
        }
    }

}
