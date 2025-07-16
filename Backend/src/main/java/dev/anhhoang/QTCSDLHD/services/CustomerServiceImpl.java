package dev.anhhoang.QTCSDLHD.services;

import dev.anhhoang.QTCSDLHD.dto.*;
import dev.anhhoang.QTCSDLHD.models.*;
import dev.anhhoang.QTCSDLHD.repositories.OrderRepository;
import dev.anhhoang.QTCSDLHD.repositories.ProductRepository;
import dev.anhhoang.QTCSDLHD.repositories.UserRepository;
import dev.anhhoang.QTCSDLHD.repositories.CartRepository;
import dev.anhhoang.QTCSDLHD.neo4j.services.RecommendationService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class CustomerServiceImpl implements CustomerService {

    // Cache để theo dõi các order request gần đây
    private final ConcurrentHashMap<String, Long> recentOrderRequests = new ConcurrentHashMap<>();
    private static final long ORDER_REQUEST_COOLDOWN_MS = 5000; // 5 giây

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private VoucherService voucherService;

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private CartCacheService cartCacheService;

    @Autowired
    private ProductCacheService productCacheService;

    @Override
    public UserProfileResponse addProductToCart(String customerId, AddToCartRequest request) {
        User user = userRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        Cart cart = cartRepository.findByCustomerId(customerId)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setCustomerId(customerId);
                    newCart.setStatus("active");
                    newCart.setCreated_at(LocalDateTime.now());
                    newCart.setUpdated_at(LocalDateTime.now());
                    newCart.setItems(new ArrayList<>());
                    return cartRepository.save(newCart);
                });

        Optional<CartItem> existingCartItem = cart.getItems().stream()
                .filter(item -> item.getProduct_id().equals(request.getProductId()))
                .findFirst();

        int newQuantity;
        if (existingCartItem.isPresent()) {
            newQuantity = existingCartItem.get().getQuantity() + request.getQuantity();
            existingCartItem.get().setQuantity(newQuantity);
        } else {
            CartItem newCartItem = new CartItem();
            newCartItem.setProduct_id(product.get_id());
            newCartItem.setQuantity(request.getQuantity());
            cart.getItems().add(newCartItem);
            newQuantity = request.getQuantity();
        }
        
        cart.setStatus("active");
        cart.setUpdated_at(LocalDateTime.now());
        cartRepository.save(cart);
        
        // Cache sản phẩm khi thêm vào giỏ hàng
        cartCacheService.cacheProductWhenAddToCart(customerId, request.getProductId(), newQuantity);
        
        return convertToUserProfileResponse(user);
    }

    @Override
    public UserProfileResponse removeProductFromCart(String customerId, RemoveFromCartRequest request) {
        User user = userRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        Cart cart = cartRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new RuntimeException("No cart found"));
        boolean removed = cart.getItems().removeIf(item -> item.getProduct_id().equals(request.getProductId()));
        if (!removed) {
            throw new RuntimeException("Product not found in cart");
        }
        cart.setStatus("active");
        cart.setUpdated_at(LocalDateTime.now());
        cartRepository.save(cart);
        
        // Xóa sản phẩm khỏi cache giỏ hàng
        cartCacheService.removeCartItemFromCache(customerId, request.getProductId());
        
        return convertToUserProfileResponse(user);
    }

    @Override
    public UserProfileResponse updateCartItemQuantity(String customerId, String productId, Integer quantity) {
        User user = userRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        Cart cart = cartRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new RuntimeException("No cart found"));
        CartItem cartItem = cart.getItems().stream()
                .filter(item -> item.getProduct_id().equals(productId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Product not found in cart"));
        if (quantity <= 0) {
            cart.getItems().remove(cartItem);
            // Xóa khỏi cache nếu quantity <= 0
            cartCacheService.removeCartItemFromCache(customerId, productId);
        } else {
            cartItem.setQuantity(quantity);
            // Cập nhật cache với quantity mới
            cartCacheService.updateCartItemQuantity(customerId, productId, quantity);
        }
        cart.setStatus("active");
        cart.setUpdated_at(LocalDateTime.now());
        cartRepository.save(cart);
        return convertToUserProfileResponse(user);
    }

    @Override
    public List<ProductResponse> getCartProducts(String customerId) {
        // Thử lấy từ cache trước
        List<CartCacheService.CartItemCache> cachedItems = cartCacheService.getAllCartItemsFromCache(customerId);
        
        if (!cachedItems.isEmpty()) {
            System.out.println("Using cached cart items for customer: " + customerId);
            return cachedItems.stream()
                    .map(cachedItem -> {
                        ProductResponse productResponse = cachedItem.getProduct();
                        productResponse.setQuantity(cachedItem.getQuantity());
                        return productResponse;
                    })
                    .collect(Collectors.toList());
        }
        
        // Nếu không có cache, lấy từ database và cache lại
        Cart cart = cartRepository.findByCustomerId(customerId)
                .orElse(null);
        if (cart == null || cart.getItems().isEmpty())
            return new ArrayList<>();
            
        List<ProductResponse> products = cart.getItems().stream()
                .map(cartItem -> productRepository.findById(cartItem.getProduct_id())
                        .map(product -> {
                            ProductResponse productResponse = new ProductResponse();
                            BeanUtils.copyProperties(product, productResponse);
                            productResponse.setQuantity(cartItem.getQuantity());
                            if (product.getShopname() != null && !product.getShopname().isEmpty()) {
                                productResponse.setShop_name(product.getShopname());
                            } else if (product.getShopid() != null) {
                                userRepository.findBySellerProfileShopId(product.getShopid()).ifPresent(sellerUser -> {
                                    if (sellerUser.getSellerProfile() != null
                                            && sellerUser.getSellerProfile().getShopName() != null) {
                                        productResponse.setShop_name(sellerUser.getSellerProfile().getShopName());
                                    }
                                });
                            }
                            return productResponse;
                        })
                        .orElseGet(() -> {
                            // If product not found in ProductRepository, return a ProductResponse with
                            // stock 0
                            // This allows the frontend to display it as out of stock/unavailable
                            ProductResponse productResponse = new ProductResponse();
                            productResponse.set_id(cartItem.getProduct_id());
                            productResponse.setName("Sản phẩm không còn tồn tại");
                            productResponse.setStock(0); // Mark as out of stock
                            productResponse.setQuantity(cartItem.getQuantity());
                            productResponse.setPrice(0.0); // Set price to 0
                            productResponse.setImage_url("/soldout.png"); // Use soldout image
                            return productResponse;
                        }))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
        
        // Cache lại dữ liệu vừa lấy từ database
        cartCacheService.syncCacheWithDatabase(customerId, cart.getItems());
        
        return products;
    }

    @Override
    @Transactional
    public String createOrderFromCart(String customerId, CreateOrderRequest request) {
        User user = userRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        if (request.getItems().isEmpty()) {
            throw new RuntimeException("Order items list is empty. Cannot create order.");
        }

        // Kiểm tra xem có đang trong thời gian chờ giữa các yêu cầu đặt hàng hay không
        Long lastRequestTime = recentOrderRequests.get(customerId);
        long currentTime = System.currentTimeMillis();
        if (lastRequestTime != null && (currentTime - lastRequestTime) < ORDER_REQUEST_COOLDOWN_MS) {
            throw new RuntimeException("Vui lòng đợi trước khi thực hiện yêu cầu đặt hàng tiếp theo.");
        }
        recentOrderRequests.put(customerId, currentTime);

        try {
            // Get shipping address from request or user profile
            String shippingAddress;
            if (!StringUtils.hasText(request.getShippingAddress())) {
                // If no address provided, get from user profile
                if (user.getBuyerProfile() == null ||
                        user.getBuyerProfile().getPrimaryAddress() == null) {
                    throw new RuntimeException("No shipping address found in profile. Please provide a shipping address.");
                }

                Address primaryAddress = user.getBuyerProfile().getPrimaryAddress();
                shippingAddress = String.format("%s, %s, %s, %s",
                        primaryAddress.getStreet(),
                        primaryAddress.getWard(),
                        primaryAddress.getDistrict(),
                        primaryAddress.getCity());
            } else {
                shippingAddress = request.getShippingAddress();
            }

        // Validate bank account for bank payment
        if ("Thẻ ngân hàng".equals(request.getPaymentMethod())) {
            if (request.getBankAccount() == null) {
                throw new RuntimeException("Bank account information is required for bank payment.");
            }
        }

        Order order = new Order();
        order.setCustomer_id(customerId);
        order.setFullName(request.getFullName());
        order.setPhoneNumber(request.getPhoneNumber());
        order.setShipping_address(shippingAddress);
        order.setPayment_method(request.getPaymentMethod());
        order.setBankAccount(request.getBankAccount());
        order.setStatus("Pending");
        order.setCreated_at(LocalDateTime.now());
        order.setUpdated_at(LocalDateTime.now());

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (CartItemRequest cartItemRequest : request.getItems()) {
            Product product = productRepository.findById(cartItemRequest.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found: " + cartItemRequest.getProductId()));

            if (product.getStock() < cartItemRequest.getQuantity()) {
                throw new RuntimeException("Not enough stock for product: " + product.getName());
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setProduct_id(product.get_id());
            orderItem.setQuantity(cartItemRequest.getQuantity());

            BigDecimal itemPrice = new BigDecimal(product.getPrice());
            String voucherId = cartItemRequest.getVoucherId();

            if (voucherId != null && !voucherId.isEmpty()) {
                Voucher voucher = voucherService.getVoucherById(voucherId);
                if (voucher != null && voucherService.isVoucherValid(voucher,
                        itemPrice.multiply(BigDecimal.valueOf(cartItemRequest.getQuantity())))) {
                    BigDecimal discount = voucherService.calculateDiscount(voucher,
                            itemPrice.multiply(BigDecimal.valueOf(cartItemRequest.getQuantity())));
                    itemPrice = itemPrice.multiply(BigDecimal.valueOf(cartItemRequest.getQuantity())).subtract(discount)
                            .divide(BigDecimal.valueOf(cartItemRequest.getQuantity()), 2,
                                    java.math.RoundingMode.HALF_UP); // Calculate discounted unit price
                    orderItem.setVoucherId(voucherId);
                }
            }

            orderItem.setPrice(itemPrice.doubleValue());
            orderItems.add(orderItem);

            total = total.add(itemPrice.multiply(BigDecimal.valueOf(cartItemRequest.getQuantity())));

            // Deduct stock
            int newStock = product.getStock() - cartItemRequest.getQuantity();
            product.setStock(newStock);
            productRepository.save(product);
            
            // Update product stock in all caches
            productCacheService.updateProductStockInCache(product.get_id(), newStock);
            cartCacheService.updateProductStockInCache(product.get_id(), newStock);
        }

        order.setItems(orderItems);

        if ("Thẻ ngân hàng".equals(request.getPaymentMethod())) {
            order.setTotal(0.0);
        } else {
            order.setTotal(total.doubleValue());
        }

        Order savedOrder = orderRepository.save(order);

        // Tracking hành vi mua sản phẩm vào Neo4j
        for (OrderItem item : orderItems) {
            recommendationService.recordProductPurchase(customerId, item.getProduct_id(), item.getQuantity());
        }

        // Remove only ordered items from cart after order creation if it's not a 'Buy
        // Now' purchase
        if (!request.isBuyNow()) {
            Cart cart = cartRepository.findByCustomerId(customerId)
                    .orElseThrow(() -> new RuntimeException("No cart found"));
            List<String> orderedProductIds = request.getItems().stream()
                    .map(CartItemRequest::getProductId)
                    .collect(Collectors.toList());
            cart.getItems().removeIf(item -> orderedProductIds.contains(item.getProduct_id()));

            // Update and save the cart status and items after removal
            cart.setStatus("active"); // Keep cart active for remaining items
            cart.setUpdated_at(LocalDateTime.now());
            cartRepository.save(cart); // Ensure the cart is saved after modifications
            
            // Remove ordered items from cache as well
            cartCacheService.removeOrderedItemsFromCache(customerId, orderedProductIds);
        }

        // Xóa cache request sau khi order thành công
        recentOrderRequests.remove(customerId);

        return savedOrder.get_id();
        
        } catch (Exception e) {
            // Xóa cache request khi có lỗi để không làm người dùng phải đợi lâu
            recentOrderRequests.remove(customerId);
            throw e;
        }
    }

    // Assuming UserProfileResponse is an existing DTO for user information
    private UserProfileResponse convertToUserProfileResponse(User user) {
        UserProfileResponse response = new UserProfileResponse();
        // Copy relevant properties from user to UserProfileResponse
        BeanUtils.copyProperties(user, response);
        return response;
    }
}