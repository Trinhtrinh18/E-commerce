package dev.anhhoang.QTCSDLHD.controllers;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.anhhoang.QTCSDLHD.dto.UserProfileResponse;
import dev.anhhoang.QTCSDLHD.models.Order;
import dev.anhhoang.QTCSDLHD.services.OrderService;
import dev.anhhoang.QTCSDLHD.services.UserService;

@RestController
@RequestMapping("/api/seller/orders")
public class SellerOrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<?> getSellerOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            Principal principal) {
        try {
            // Lấy thông tin seller từ token
            UserProfileResponse userProfile = userService.findUserProfileByEmail(principal.getName());
            String sellerId = userProfile.getId();
            
            // Lấy danh sách orders của seller
            List<Order> orders = orderService.getOrdersBySeller(sellerId, status, startDate, endDate);
            
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching seller orders: " + e.getMessage());
        }
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrderDetail(@PathVariable String orderId, Principal principal) {
        try {
            UserProfileResponse userProfile = userService.findUserProfileByEmail(principal.getName());
            String sellerId = userProfile.getId();
            
            Order order = orderService.getOrderDetailForSeller(orderId, sellerId);
            
            if (order == null) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching order detail: " + e.getMessage());
        }
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable String orderId,
            @RequestBody Map<String, String> request,
            Principal principal) {
        try {
            UserProfileResponse userProfile = userService.findUserProfileByEmail(principal.getName());
            String sellerId = userProfile.getId();
            
            String newStatus = request.get("status");
            
            Order updatedOrder = orderService.updateOrderStatusForSeller(orderId, sellerId, newStatus);
            
            if (updatedOrder == null) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(updatedOrder);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error updating order status: " + e.getMessage());
        }
    }

    @GetMapping("/statistics")
    public ResponseEntity<?> getOrderStatistics(Principal principal) {
        try {
            UserProfileResponse userProfile = userService.findUserProfileByEmail(principal.getName());
            String sellerId = userProfile.getId();
            
            Map<String, Object> statistics = orderService.getOrderStatisticsBySeller(sellerId);
            
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching order statistics: " + e.getMessage());
        }
    }
}
