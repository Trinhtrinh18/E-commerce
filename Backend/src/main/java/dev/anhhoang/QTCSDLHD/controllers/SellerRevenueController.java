package dev.anhhoang.QTCSDLHD.controllers;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.anhhoang.QTCSDLHD.dto.UserProfileResponse;
import dev.anhhoang.QTCSDLHD.services.OrderService;
import dev.anhhoang.QTCSDLHD.services.UserService;

@RestController
@RequestMapping("/api/seller/revenue")
public class SellerRevenueController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserService userService;

    @GetMapping("/overview")
    public ResponseEntity<?> getRevenueOverview(Principal principal) {
        try {
            UserProfileResponse userProfile = userService.findUserProfileByEmail(principal.getName());
            String sellerId = userProfile.getId();
            
            Map<String, Object> statistics = orderService.getOrderStatisticsBySeller(sellerId);
            
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching revenue overview: " + e.getMessage());
        }
    }

    @GetMapping("/chart")
    public ResponseEntity<?> getRevenueChart(
            @RequestParam(required = false, defaultValue = "monthly") String period,
            Principal principal) {
        try {
            UserProfileResponse userProfile = userService.findUserProfileByEmail(principal.getName());
            String sellerId = userProfile.getId();
            
            List<Map<String, Object>> chartData = orderService.getOrderChartDataBySeller(sellerId, period);
            
            Map<String, Object> response = Map.of(
                "period", period,
                "data", chartData
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching chart data: " + e.getMessage());
        }
    }
}
