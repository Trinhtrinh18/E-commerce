package dev.anhhoang.QTCSDLHD.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dev.anhhoang.QTCSDLHD.dto.OrderDTO;
import dev.anhhoang.QTCSDLHD.dto.OrderItemDTO;
import dev.anhhoang.QTCSDLHD.models.Order;
import dev.anhhoang.QTCSDLHD.models.OrderItem;
import dev.anhhoang.QTCSDLHD.models.Product;
import dev.anhhoang.QTCSDLHD.models.User;
import dev.anhhoang.QTCSDLHD.repositories.OrderRepository;
import dev.anhhoang.QTCSDLHD.repositories.ProductRepository;
import dev.anhhoang.QTCSDLHD.repositories.UserRepository;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public boolean cancelOrderByCustomer(String orderId, String customerId) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return false;
        }
        Order order = orderOpt.get();
        // Check ownership and status
        String status = order.getStatus();
        if (!order.getCustomer_id().equals(customerId) || !("PENDING".equalsIgnoreCase(status) || "CONFIRMED".equalsIgnoreCase(status))) {
            return false;
        }
        // Restore stock for each item
        for (OrderItem item : order.getItems()) {
            Optional<Product> productOpt = productRepository.findById(item.getProduct_id());
            if (productOpt.isPresent()) {
                Product product = productOpt.get();
                product.setStock(product.getStock() + item.getQuantity());
                productRepository.save(product);
            }
        }
        // Update order status
        order.setStatus("CANCELLED");
        orderRepository.save(order);
        return true;
    }

    @Override
    public List<Order> getOrdersBySeller(String sellerId, String status, String startDate, String endDate) {
        // Lấy user để get shopId từ sellerProfile
        Optional<User> userOpt = userRepository.findById(sellerId);
        if (userOpt.isEmpty() || userOpt.get().getSellerProfile() == null) {
            return new ArrayList<>();
        }
        
        String shopId = userOpt.get().getSellerProfile().getShopId();
        
        // Lấy tất cả orders
        List<Order> allOrders = orderRepository.findAll();
        
        // Lấy tất cả products của shop này
        List<Product> sellerProducts = productRepository.findByShopid(shopId);
        
        Set<String> sellerProductIds = sellerProducts.stream()
                .map(Product::get_id)
                .collect(Collectors.toSet());
        
        // Filter orders có chứa products của seller
        List<Order> sellerOrders = allOrders.stream()
                .filter(order -> {
                    return order.getItems().stream()
                            .anyMatch(item -> sellerProductIds.contains(item.getProduct_id()));
                })
                .collect(Collectors.toList());
        
        // Apply filters
        if (status != null && !status.isEmpty()) {
            sellerOrders = sellerOrders.stream()
                    .filter(order -> status.equalsIgnoreCase(order.getStatus()))
                    .collect(Collectors.toList());
        }

        if (startDate != null && !startDate.isEmpty()) {
            LocalDate start = LocalDate.parse(startDate);
            sellerOrders = sellerOrders.stream()
                    .filter(order -> order.getCreated_at().toLocalDate().isAfter(start.minusDays(1)))
                    .collect(Collectors.toList());
        }

        if (endDate != null && !endDate.isEmpty()) {
            LocalDate end = LocalDate.parse(endDate);
            sellerOrders = sellerOrders.stream()
                    .filter(order -> order.getCreated_at().toLocalDate().isBefore(end.plusDays(1)))
                    .collect(Collectors.toList());
        }

        // Sort by created_at descending
        sellerOrders.sort((o1, o2) -> o2.getCreated_at().compareTo(o1.getCreated_at()));

        return sellerOrders;
    }

    @Override
    public Order getOrderDetailForSeller(String orderId, String sellerId) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return null;
        }
        
        Order order = orderOpt.get();
        
        // Lấy user để get shopId từ sellerProfile
        Optional<User> userOpt = userRepository.findById(sellerId);
        if (userOpt.isEmpty() || userOpt.get().getSellerProfile() == null) {
            return null;
        }
        
        String shopId = userOpt.get().getSellerProfile().getShopId();
        
        // Kiểm tra xem order có chứa products của seller không
        List<Product> sellerProducts = productRepository.findByShopid(shopId);
        Set<String> sellerProductIds = sellerProducts.stream()
                .map(Product::get_id)
                .collect(Collectors.toSet());
        
        boolean hasSellerProducts = order.getItems().stream()
                .anyMatch(item -> sellerProductIds.contains(item.getProduct_id()));
        
        if (!hasSellerProducts) {
            return null; // Seller không có quyền xem order này
        }
        
        return order;
    }

    @Override
    public Order updateOrderStatusForSeller(String orderId, String sellerId, String newStatus) {
        Order order = getOrderDetailForSeller(orderId, sellerId);
        if (order == null) {
            return null;
        }

        // Validate status transition (có thể thêm business logic ở đây)
        List<String> validStatuses = Arrays.asList("PENDING", "CONFIRMED", "SHIPPING", "DELIVERED", "CANCELLED");
        if (!validStatuses.contains(newStatus)) {
            throw new IllegalArgumentException("Invalid status: " + newStatus);
        }

        // Nếu chuyển sang CANCELLED thì cộng lại số lượng sản phẩm
        if ("CANCELLED".equalsIgnoreCase(newStatus) && !"CANCELLED".equalsIgnoreCase(order.getStatus())) {
            for (OrderItem item : order.getItems()) {
                Optional<Product> productOpt = productRepository.findById(item.getProduct_id());
                if (productOpt.isPresent()) {
                    Product product = productOpt.get();
                    product.setStock(product.getStock() + item.getQuantity());
                    productRepository.save(product);
                }
            }
        }

        order.setStatus(newStatus);
        order.setUpdated_at(LocalDateTime.now());

        return orderRepository.save(order);
    }

    @Override
    public Map<String, Object> getOrderStatisticsBySeller(String sellerId) {
        List<Order> sellerOrders = getOrdersBySeller(sellerId, null, null, null);
        
        Map<String, Object> statistics = new HashMap<>();
        
        // Tổng số orders
        statistics.put("totalOrders", sellerOrders.size());
        
        // Số orders đã hoàn thành (DELIVERED)
        long completedOrders = sellerOrders.stream()
                .filter(order -> "DELIVERED".equalsIgnoreCase(order.getStatus()))
                .count();
        statistics.put("completedOrders", completedOrders);
        
        // Phân bố theo status (normalize thành uppercase)
        Map<String, Long> statusDistribution = sellerOrders.stream()
                .collect(Collectors.groupingBy(
                    order -> order.getStatus().toUpperCase(), 
                    Collectors.counting()
                ));
        statistics.put("statusDistribution", statusDistribution);
        
        // Lấy shopId để tính revenue
        Optional<User> userOpt = userRepository.findById(sellerId);
        if (userOpt.isEmpty() || userOpt.get().getSellerProfile() == null) {
            statistics.put("totalRevenue", 0.0);
            statistics.put("recentOrders", 0L);
            return statistics;
        }
        
        String shopId = userOpt.get().getSellerProfile().getShopId();
        List<Product> sellerProducts = productRepository.findByShopid(shopId);
        Set<String> sellerProductIds = sellerProducts.stream()
                .map(Product::get_id)
                .collect(Collectors.toSet());
        
        // Tổng doanh thu (chỉ tính từ đơn hàng đã giao thành công)
        double totalRevenue = sellerOrders.stream()
                .filter(order -> "DELIVERED".equalsIgnoreCase(order.getStatus())) // Chỉ tính đơn đã giao
                .mapToDouble(order -> {
                    return order.getItems().stream()
                            .filter(item -> sellerProductIds.contains(item.getProduct_id()))
                            .mapToDouble(item -> item.getPrice() * item.getQuantity())
                            .sum();
                })
                .sum();
        statistics.put("totalRevenue", totalRevenue);
        
        // Orders trong 30 ngày qua
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        long recentOrders = sellerOrders.stream()
                .filter(order -> order.getCreated_at().isAfter(thirtyDaysAgo))
                .count();
        statistics.put("recentOrders", recentOrders);
        
        return statistics;
    }

    @Override
    public List<Map<String, Object>> getOrderChartDataBySeller(String sellerId, String period) {
        List<Order> sellerOrders = getOrdersBySeller(sellerId, null, null, null);
        List<Map<String, Object>> chartData = new ArrayList<>();
        
        if ("monthly".equals(period)) {
            // Group by months for the current year
            Map<String, Map<String, Object>> monthlyData = new LinkedHashMap<>();
            
            // Initialize all months with 0 orders and revenue
            String[] months = {"T1", "T2", "T3", "T4", "T5", "T6", "T7", "T8", "T9", "T10", "T11", "T12"};
            for (String month : months) {
                Map<String, Object> data = new HashMap<>();
                data.put("name", month);
                data.put("orders", 0);
                data.put("completedOrders", 0);
                data.put("cancelledOrders", 0);
                data.put("revenue", 0.0);
                monthlyData.put(month, data);
            }
            
            // Get shopId để tính revenue
            Optional<User> userOpt = userRepository.findById(sellerId);
            Set<String> sellerProductIds = Set.of();
            if (userOpt.isPresent() && userOpt.get().getSellerProfile() != null) {
                String shopId = userOpt.get().getSellerProfile().getShopId();
                List<Product> sellerProducts = productRepository.findByShopid(shopId);
                sellerProductIds = sellerProducts.stream()
                        .map(Product::get_id)
                        .collect(Collectors.toSet());
            }
            
            final Set<String> finalSellerProductIds = sellerProductIds;
            
            // Process actual orders
            for (Order order : sellerOrders) {
                int month = order.getCreated_at().getMonthValue();
                String monthKey = "T" + month;
                
                Map<String, Object> monthData = monthlyData.get(monthKey);
                if (monthData != null) {
                    // Count all orders (not just delivered ones)
                    int currentOrders = (Integer) monthData.get("orders");
                    monthData.put("orders", currentOrders + 1);
                    
                    // Count completed orders (DELIVERED)
                    if ("DELIVERED".equalsIgnoreCase(order.getStatus())) {
                        int currentCompleted = (Integer) monthData.get("completedOrders");
                        monthData.put("completedOrders", currentCompleted + 1);
                        
                        // Revenue only from delivered orders
                        double orderRevenue = order.getItems().stream()
                                .filter(item -> finalSellerProductIds.contains(item.getProduct_id()))
                                .mapToDouble(item -> item.getPrice() * item.getQuantity())
                                .sum();
                        
                        double currentRevenue = (Double) monthData.get("revenue");
                        monthData.put("revenue", currentRevenue + orderRevenue);
                    }
                    
                    // Count cancelled orders
                    if ("CANCELLED".equalsIgnoreCase(order.getStatus())) {
                        int currentCancelled = (Integer) monthData.get("cancelledOrders");
                        monthData.put("cancelledOrders", currentCancelled + 1);
                    }
                }
            }
            
            chartData.addAll(monthlyData.values());
            
        } else if ("weekly".equals(period)) {
            // Group by weeks for the last 12 weeks
            Map<String, Map<String, Object>> weeklyData = new LinkedHashMap<>();
            LocalDate now = LocalDate.now();
            
            // Initialize last 12 weeks
            for (int i = 11; i >= 0; i--) {
                LocalDate weekStart = now.minusWeeks(i).with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
                LocalDate weekEnd = weekStart.plusDays(6);
                
                // Format ngắn gọn: "T25/6-1/7"
                String weekName = String.format("T%d/%d-%d/%d", 
                    weekStart.getDayOfMonth(), weekStart.getMonthValue(),
                    weekEnd.getDayOfMonth(), weekEnd.getMonthValue());
                String weekKey = weekName;
                
                Map<String, Object> data = new HashMap<>();
                data.put("name", weekName);
                data.put("orders", 0);
                data.put("completedOrders", 0);
                data.put("cancelledOrders", 0);
                data.put("revenue", 0.0);
                data.put("weekStart", weekStart);
                weeklyData.put(weekKey, data);
            }
            
            // Get shopId để tính revenue
            Optional<User> userOpt = userRepository.findById(sellerId);
            Set<String> sellerProductIds = Set.of();
            if (userOpt.isPresent() && userOpt.get().getSellerProfile() != null) {
                String shopId = userOpt.get().getSellerProfile().getShopId();
                List<Product> sellerProducts = productRepository.findByShopid(shopId);
                sellerProductIds = sellerProducts.stream()
                        .map(Product::get_id)
                        .collect(Collectors.toSet());
            }
            
            final Set<String> finalSellerProductIds = sellerProductIds;
            
            // Process actual orders
            for (Order order : sellerOrders) {
                LocalDate orderDate = order.getCreated_at().toLocalDate();
                
                // Find which week this order belongs to
                for (Map.Entry<String, Map<String, Object>> weekEntry : weeklyData.entrySet()) {
                    LocalDate weekStart = (LocalDate) weekEntry.getValue().get("weekStart");
                    LocalDate weekEnd = weekStart.plusDays(6);
                    
                    if (!orderDate.isBefore(weekStart) && !orderDate.isAfter(weekEnd)) {
                        Map<String, Object> weekData = weekEntry.getValue();
                        
                        // Count all orders (not just delivered ones)
                        int currentOrders = (Integer) weekData.get("orders");
                        weekData.put("orders", currentOrders + 1);
                        
                        // Count completed orders (DELIVERED)
                        if ("DELIVERED".equalsIgnoreCase(order.getStatus())) {
                            int currentCompleted = (Integer) weekData.get("completedOrders");
                            weekData.put("completedOrders", currentCompleted + 1);
                            
                            // Revenue only from delivered orders
                            double orderRevenue = order.getItems().stream()
                                    .filter(item -> finalSellerProductIds.contains(item.getProduct_id()))
                                    .mapToDouble(item -> item.getPrice() * item.getQuantity())
                                    .sum();
                            
                            double currentRevenue = (Double) weekData.get("revenue");
                            weekData.put("revenue", currentRevenue + orderRevenue);
                        }
                        
                        // Count cancelled orders
                        if ("CANCELLED".equalsIgnoreCase(order.getStatus())) {
                            int currentCancelled = (Integer) weekData.get("cancelledOrders");
                            weekData.put("cancelledOrders", currentCancelled + 1);
                        }
                        break;
                    }
                }
            }
            
            // Remove weekStart field before returning
            for (Map<String, Object> weekData : weeklyData.values()) {
                weekData.remove("weekStart");
            }
            
            chartData.addAll(weeklyData.values());
            
        } else if ("yearly".equals(period)) {
            // Group by years for the last 5 years
            Map<String, Map<String, Object>> yearlyData = new LinkedHashMap<>();
            int currentYear = LocalDate.now().getYear();
            
            // Initialize last 5 years
            for (int i = 4; i >= 0; i--) {
                int year = currentYear - i;
                String yearKey = "Năm " + year;
                
                Map<String, Object> data = new HashMap<>();
                data.put("name", yearKey);
                data.put("orders", 0);
                data.put("completedOrders", 0);
                data.put("cancelledOrders", 0);
                data.put("revenue", 0.0);
                yearlyData.put(yearKey, data);
            }
            
            // Get shopId để tính revenue
            Optional<User> userOpt = userRepository.findById(sellerId);
            Set<String> sellerProductIds = Set.of();
            if (userOpt.isPresent() && userOpt.get().getSellerProfile() != null) {
                String shopId = userOpt.get().getSellerProfile().getShopId();
                List<Product> sellerProducts = productRepository.findByShopid(shopId);
                sellerProductIds = sellerProducts.stream()
                        .map(Product::get_id)
                        .collect(Collectors.toSet());
            }
            
            final Set<String> finalSellerProductIds = sellerProductIds;
            
            // Process actual orders
            for (Order order : sellerOrders) {
                int orderYear = order.getCreated_at().getYear();
                String yearKey = "Năm " + orderYear;
                
                Map<String, Object> yearData = yearlyData.get(yearKey);
                if (yearData != null) {
                    // Count all orders (not just delivered ones)
                    int currentOrders = (Integer) yearData.get("orders");
                    yearData.put("orders", currentOrders + 1);
                    
                    // Count completed orders (DELIVERED)
                    if ("DELIVERED".equalsIgnoreCase(order.getStatus())) {
                        int currentCompleted = (Integer) yearData.get("completedOrders");
                        yearData.put("completedOrders", currentCompleted + 1);
                        
                        // Revenue only from delivered orders
                        double orderRevenue = order.getItems().stream()
                                .filter(item -> finalSellerProductIds.contains(item.getProduct_id()))
                                .mapToDouble(item -> item.getPrice() * item.getQuantity())
                                .sum();
                        
                        double currentRevenue = (Double) yearData.get("revenue");
                        yearData.put("revenue", currentRevenue + orderRevenue);
                    }
                    
                    // Count cancelled orders
                    if ("CANCELLED".equalsIgnoreCase(order.getStatus())) {
                        int currentCancelled = (Integer) yearData.get("cancelledOrders");
                        yearData.put("cancelledOrders", currentCancelled + 1);
                    }
                }
            }
            
            chartData.addAll(yearlyData.values());
        }
        
        return chartData;
    }
    
    @Override
    public List<OrderDTO> getOrdersByCustomer(String customerId, String status) {
        List<Order> customerOrders = orderRepository.findByCustomer_id(customerId);

        if (status != null && !status.isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            customerOrders = customerOrders.stream()
                    .filter(order -> status.equalsIgnoreCase(order.getStatus()))
                    .collect(Collectors.toList());
        }

        List<String> allProductIds = customerOrders.stream()
                .flatMap(order -> order.getItems().stream())
                .map(OrderItem::getProduct_id)
                .distinct()
                .collect(Collectors.toList());

        Map<String, Product> productMap = productRepository.findAllById(allProductIds)
                .stream()
                .collect(Collectors.toMap(Product::get_id, p -> p));

        List<OrderDTO> orderDTOs = new ArrayList<>();
        for (Order order : customerOrders) {
            OrderDTO dto = new OrderDTO();
            dto.set_id(order.get_id());
            dto.setCustomer_id(order.getCustomer_id());
            dto.setFullName(order.getFullName());
            dto.setPhoneNumber(order.getPhoneNumber());
            dto.setShipping_address(order.getShipping_address());
            dto.setPayment_method(order.getPayment_method());
            dto.setStatus(order.getStatus() != null ? order.getStatus().toUpperCase() : null);
            dto.setTotal(order.getTotal());
            dto.setCreated_at(order.getCreated_at());
            dto.setUpdated_at(order.getUpdated_at());

            List<OrderItemDTO> itemDTOs = new ArrayList<>();
            for (OrderItem item : order.getItems()) {
                OrderItemDTO itemDTO = new OrderItemDTO();
                itemDTO.setProduct_id(item.getProduct_id());
                itemDTO.setQuantity(item.getQuantity());
                itemDTO.setPrice(item.getPrice());
                itemDTO.setVoucherId(item.getVoucherId());
                Product product = productMap.get(item.getProduct_id());
                if (product != null) {
                    itemDTO.setProduct_name(product.getName());
                    itemDTO.setImage_url(product.getImage_url());
                }
                itemDTOs.add(itemDTO);
            }
            dto.setItems(itemDTOs);
            orderDTOs.add(dto);
        }

        orderDTOs.sort((o1, o2) -> o2.getCreated_at().compareTo(o1.getCreated_at()));
        return orderDTOs;
    }
    
    @Override
    public OrderDTO getOrderDetailForCustomer(String orderId, String customerId) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return null;
        }
        Order order = orderOpt.get();
        if (!customerId.equals(order.getCustomer_id())) {
            return null;
        }
        // Build OrderDTO giống như getOrdersByCustomer
        OrderDTO dto = new OrderDTO();
        dto.set_id(order.get_id());
        dto.setCustomer_id(order.getCustomer_id());
        dto.setFullName(order.getFullName());
        dto.setPhoneNumber(order.getPhoneNumber());
        dto.setShipping_address(order.getShipping_address());
        dto.setPayment_method(order.getPayment_method());
        dto.setStatus(order.getStatus() != null ? order.getStatus().toUpperCase() : null);
        dto.setTotal(order.getTotal());
        dto.setCreated_at(order.getCreated_at());
        dto.setUpdated_at(order.getUpdated_at());
        // Map items to OrderItemDTO, lấy product_name, image_url
        List<OrderItemDTO> itemDTOs = new ArrayList<>();
        if (order.getItems() != null) {
            for (var item : order.getItems()) {
                OrderItemDTO itemDTO = new OrderItemDTO();
                itemDTO.setProduct_id(item.getProduct_id());
                itemDTO.setQuantity(item.getQuantity());
                itemDTO.setPrice(item.getPrice());
                itemDTO.setVoucherId(item.getVoucherId());
                // Lấy thông tin sản phẩm
                Optional<Product> productOpt = productRepository.findById(item.getProduct_id());
                if (productOpt.isPresent()) {
                    Product product = productOpt.get();
                    itemDTO.setProduct_name(product.getName());
                    itemDTO.setImage_url(product.getImage_url());
                }
                itemDTOs.add(itemDTO);
            }
        }
        dto.setItems(itemDTOs);
        return dto;
    }

    @Override
    public boolean confirmOrderDelivered(String orderId, String customerId) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) return false;
        Order order = orderOpt.get();
        if (!customerId.equals(order.getCustomer_id())) return false;
        if (!"SHIPPING".equalsIgnoreCase(order.getStatus())) return false;
        order.setStatus("DELIVERED");
        order.setUpdated_at(LocalDateTime.now());
        orderRepository.save(order);
        return true;
    }
}
