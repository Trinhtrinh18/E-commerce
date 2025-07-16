package dev.anhhoang.QTCSDLHD.controllers;

import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.anhhoang.QTCSDLHD.dto.OrderDTO;
import dev.anhhoang.QTCSDLHD.dto.UserProfileResponse;
import dev.anhhoang.QTCSDLHD.services.OrderService;
import dev.anhhoang.QTCSDLHD.services.UserService;

@RestController
@RequestMapping("/api/customer/orders")
public class CustomerOrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<?> getCustomerOrders(
            @RequestParam(required = false, defaultValue = "ALL") String status,
            Principal principal) {
        try {
            // Lấy thông tin customer từ token
            UserProfileResponse userProfile = userService.findUserProfileByEmail(principal.getName());
            String customerId = userProfile.getId();

            // Lấy danh sách orders của customer (dùng DTO)
            List<OrderDTO> orders = orderService.getOrdersByCustomer(customerId, status);

            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching customer orders: " + e.getMessage());
        }
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrderDetail(@PathVariable String orderId, Principal principal) {
        try {
            UserProfileResponse userProfile = userService.findUserProfileByEmail(principal.getName());
            String customerId = userProfile.getId();
            OrderDTO order = orderService.getOrderDetailForCustomer(orderId, customerId);
            if (order == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching order detail: " + e.getMessage());
        }
    }

    @PostMapping("/{orderId}/confirm-delivery")
    public ResponseEntity<?> confirmOrderDelivered(@PathVariable String orderId, Principal principal) {
        try {
            UserProfileResponse userProfile = userService.findUserProfileByEmail(principal.getName());
            String customerId = userProfile.getId();
            boolean success = orderService.confirmOrderDelivered(orderId, customerId);
            if (!success) {
                return ResponseEntity.badRequest().body("Không thể xác nhận đã nhận hàng. Đơn hàng không hợp lệ hoặc không ở trạng thái Đang giao hàng.");
            }
            return ResponseEntity.ok("Đã xác nhận nhận hàng thành công.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi xác nhận nhận hàng: " + e.getMessage());
        }
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable String orderId, Principal principal) {
        try {
            UserProfileResponse userProfile = userService.findUserProfileByEmail(principal.getName());
            String customerId = userProfile.getId();
            boolean success = orderService.cancelOrderByCustomer(orderId, customerId);
            if (!success) {
                return ResponseEntity.badRequest().body("Không thể hủy đơn hàng. Đơn hàng không hợp lệ hoặc không ở trạng thái Chờ xác nhận.");
            }
            return ResponseEntity.ok("Đã hủy đơn hàng thành công và đã hoàn lại số lượng sản phẩm.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi hủy đơn hàng: " + e.getMessage());
        }
    }
}
