package dev.anhhoang.QTCSDLHD.controllers;

import dev.anhhoang.QTCSDLHD.dto.ApiResponse;
import dev.anhhoang.QTCSDLHD.dto.ProductResponse;
import dev.anhhoang.QTCSDLHD.neo4j.services.RecommendationService;
import dev.anhhoang.QTCSDLHD.dto.UserProfileResponse;
import dev.anhhoang.QTCSDLHD.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getRecommendations(java.security.Principal principal) {
        try {
            if (principal == null) {
                return ResponseEntity.badRequest().body(new ApiResponse<>(false, "User not authenticated", null));
            }

            UserProfileResponse userProfile = userService.findUserProfileByEmail(principal.getName());
            String userId = userProfile.getId();

            List<ProductResponse> recommendations = recommendationService.getRecommendedProductsForUser(userId);

            return ResponseEntity
                    .ok(new ApiResponse<>(true, "Recommendations retrieved successfully", recommendations));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Error retrieving recommendations: " + e.getMessage(), null));
        }
    }

    @GetMapping("/similar/{productId}")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getSimilarProducts(
            @PathVariable String productId,
            java.security.Principal principal) {
        try {
            if (principal == null) {
                return ResponseEntity.badRequest().body(new ApiResponse<>(false, "User not authenticated", null));
            }

            UserProfileResponse userProfile = userService.findUserProfileByEmail(principal.getName());
            String userId = userProfile.getId();

            List<ProductResponse> similarProducts = recommendationService.getSimilarProductsForProduct(productId,
                    userId);

            return ResponseEntity
                    .ok(new ApiResponse<>(true, "Similar products retrieved successfully", similarProducts));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Error retrieving similar products: " + e.getMessage(), null));
        }
    }
}