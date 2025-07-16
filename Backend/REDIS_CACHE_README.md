# Redis Cache Implementation for Shopping Cart

## Tổng quan

Dự án đã được tích hợp Redis cache để cải thiện hiệu suất khi thêm sản phẩm vào giỏ hàng. Hệ thống cache sẽ lưu trữ thông tin sản phẩm và giỏ hàng để giảm thiểu việc truy vấn database.

## Tính năng Cache

### 1. Product Cache
- **Tự động cache** sản phẩm khi thêm vào giỏ hàng
- **TTL: 24 giờ** - cache sẽ tự động expire
- **Key pattern**: `product:{productId}`

### 2. Cart Items Cache
- **Cache chi tiết item** trong giỏ hàng của từng khách hàng
- **TTL: 12 giờ** - cache sẽ tự động expire
- **Key pattern**: `cart_items:{customerId}:{productId}`

## Cấu hình Redis

### Redis Cloud Connection
```properties
# Redis configuration
spring.data.redis.host=redis-12817.c15.us-east-1-2.ec2.redns.redis-cloud.com
spring.data.redis.port=12817
spring.data.redis.username=default
spring.data.redis.password=gfIR0OPV7ndUviFO3epkJT5fPl69cpyO
```

### Dependencies được thêm vào pom.xml
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
```

## Cách hoạt động

### 1. Thêm sản phẩm vào giỏ hàng
```java
// Khi thêm sản phẩm vào giỏ hàng
customerService.addProductToCart(customerId, request);
// → Tự động cache sản phẩm và thông tin giỏ hàng
```

### 2. Lấy sản phẩm từ giỏ hàng
```java
// Ưu tiên lấy từ cache trước
List<CartItemCache> cachedItems = cartCacheService.getAllCartItemsFromCache(customerId);
// Nếu cache miss → lấy từ database và cache lại
```

### 3. Cập nhật số lượng
```java
// Cập nhật cả database và cache
cartCacheService.updateCartItemQuantity(customerId, productId, quantity);
```

### 4. Xóa sản phẩm khỏi giỏ hàng
```java
// Xóa cả database và cache
cartCacheService.removeCartItemFromCache(customerId, productId);
```

## API Endpoints để quản lý Cache

### 1. Kiểm tra cache status
```
GET /api/cache/status
```

### 2. Lấy thống kê cache
```
GET /api/cache/statistics
```

### 3. Kiểm tra kết nối Redis
```
GET /api/cache/health
```

### 4. Lấy cached cart items
```
GET /api/cache/cart-items
```

### 5. Xóa cache giỏ hàng
```
DELETE /api/cache/cart/clear
```

### 6. Kiểm tra sản phẩm trong cache
```
GET /api/cache/product/{productId}/exists
```

## Lợi ích của Cache

1. **Cải thiện hiệu suất**: Giảm thời gian phản hồi khi lấy thông tin giỏ hàng
2. **Giảm tải database**: Ít truy vấn database hơn
3. **Trải nghiệm người dùng tốt hơn**: Trang web phản hồi nhanh hơn
4. **Khả năng mở rộng**: Hỗ trợ nhiều người dùng đồng thời

## Monitoring và Debug

### Xem log cache
```
# Khi cache hit
Product found in cache: {productId}

# Khi cache miss
Product cached from database: {productId}

# Khi cache sản phẩm vào giỏ hàng
Cached product when adding to cart: {productId} for customer: {customerId}
```

### Thống kê cache
- **Product cache count**: Số lượng sản phẩm được cache
- **Cart items cache count**: Số lượng items trong giỏ hàng được cache
- **Total cache keys**: Tổng số keys trong Redis

## Troubleshooting

### 1. Kết nối Redis thất bại
- Kiểm tra thông tin kết nối trong `application.properties`
- Kiểm tra firewall và network
- Sử dụng endpoint `/api/cache/health` để test

### 2. Cache không hoạt động
- Kiểm tra Redis service đang chạy
- Xem log application để tìm lỗi
- Kiểm tra cấu hình `RedisConfig`

### 3. Dữ liệu cache không đồng bộ
- Sử dụng endpoint `/api/cache/cart/clear` để xóa cache
- Hệ thống sẽ tự động đồng bộ lại từ database

## Lưu ý quan trọng

1. **TTL**: Cache sẽ tự động expire, không cần lo lắng về việc dữ liệu cũ
2. **Consistency**: Hệ thống đảm bảo đồng bộ giữa cache và database
3. **Fallback**: Nếu cache fail, hệ thống vẫn hoạt động bình thường từ database
4. **Security**: Chỉ cache dữ liệu không nhạy cảm

## Testing

Để test cache đang hoạt động:
1. Thêm sản phẩm vào giỏ hàng
2. Gọi API `/api/cache/status` để xem cache
3. Gọi API `/api/cache/statistics` để xem thống kê
4. Kiểm tra log để thấy cache hit/miss
