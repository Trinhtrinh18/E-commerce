package dev.anhhoang.QTCSDLHD.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class CacheMonitoringService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * Thống kê cache
     */
    public CacheStatistics getCacheStatistics() {
        CacheStatistics stats = new CacheStatistics();
        
        // Đếm số lượng keys theo pattern
        Set<String> productKeys = redisTemplate.keys("product:*");
        Set<String> cartKeys = redisTemplate.keys("cart_items:*");
        
        stats.setProductCacheCount(productKeys != null ? productKeys.size() : 0);
        stats.setCartItemsCacheCount(cartKeys != null ? cartKeys.size() : 0);
        stats.setTotalCacheKeys(stats.getProductCacheCount() + stats.getCartItemsCacheCount());
        
        return stats;
    }

    /**
     * Xóa tất cả cache (chỉ sử dụng khi cần thiết)
     */
    public void clearAllCache() {
        Set<String> allKeys = redisTemplate.keys("*");
        if (allKeys != null && !allKeys.isEmpty()) {
            redisTemplate.delete(allKeys);
        }
    }

    /**
     * Lấy thời gian TTL của một key
     */
    public long getKeyTTL(String key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    /**
     * Kiểm tra kết nối Redis
     */
    public boolean isRedisConnected() {
        try {
            redisTemplate.opsForValue().get("test");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Inner class cho thống kê cache
     */
    public static class CacheStatistics {
        private int productCacheCount;
        private int cartItemsCacheCount;
        private int totalCacheKeys;

        // Getters and setters
        public int getProductCacheCount() { return productCacheCount; }
        public void setProductCacheCount(int productCacheCount) { this.productCacheCount = productCacheCount; }

        public int getCartItemsCacheCount() { return cartItemsCacheCount; }
        public void setCartItemsCacheCount(int cartItemsCacheCount) { this.cartItemsCacheCount = cartItemsCacheCount; }

        public int getTotalCacheKeys() { return totalCacheKeys; }
        public void setTotalCacheKeys(int totalCacheKeys) { this.totalCacheKeys = totalCacheKeys; }
    }
}
