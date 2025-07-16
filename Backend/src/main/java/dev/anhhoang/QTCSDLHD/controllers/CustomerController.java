package dev.anhhoang.QTCSDLHD.controllers;

import dev.anhhoang.QTCSDLHD.dto.*;
import dev.anhhoang.QTCSDLHD.services.CustomerService;
import dev.anhhoang.QTCSDLHD.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private UserService userService;

    private String getCustomerId(Principal principal) {
        UserProfileResponse userProfile = userService.findUserProfileByEmail(principal.getName());
        if (userProfile == null || userProfile.getId() == null) {
            throw new RuntimeException("User profile not found or ID missing.");
        }
        return userProfile.getId();
    }

    @PostMapping("/cart/add")
    public ResponseEntity<String> addProductToCart(@Valid @RequestBody AddToCartRequest request,
            Principal principal) {
        try {
            String customerId = getCustomerId(principal);
            customerService.addProductToCart(customerId, request);
            return ResponseEntity.ok("Product added to cart successfully!");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @DeleteMapping("/cart/remove")
    public ResponseEntity<String> removeProductFromCart(@Valid @RequestBody RemoveFromCartRequest request,
            Principal principal) {
        try {
            String customerId = getCustomerId(principal);
            customerService.removeProductFromCart(customerId, request);
            return ResponseEntity.ok("Product removed from cart successfully!");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @PutMapping("/cart/update-quantity/{productId}/{quantity}")
    public ResponseEntity<String> updateCartItemQuantity(@PathVariable String productId,
            @PathVariable Integer quantity, Principal principal) {
        try {
            String customerId = getCustomerId(principal);
            customerService.updateCartItemQuantity(customerId, productId, quantity);
            return ResponseEntity.ok("Cart item quantity updated successfully!");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @GetMapping("/cart")
    public ResponseEntity<?> getCartProducts(Principal principal) {
        try {
            String customerId = getCustomerId(principal);
            List<ProductResponse> products = customerService.getCartProducts(customerId);
            return ResponseEntity.ok(products);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @PostMapping("/orders")
    public ResponseEntity<String> createOrderFromCart(@Valid @RequestBody CreateOrderRequest request,
            Principal principal) {
        try {
            String customerId = getCustomerId(principal);
            String orderId = customerService.createOrderFromCart(customerId, request);
            return ResponseEntity.ok(orderId);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(null);
        }
    }
}