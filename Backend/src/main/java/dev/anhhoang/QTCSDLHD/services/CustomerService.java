package dev.anhhoang.QTCSDLHD.services;

import dev.anhhoang.QTCSDLHD.dto.AddToCartRequest;
import dev.anhhoang.QTCSDLHD.dto.RemoveFromCartRequest;
import dev.anhhoang.QTCSDLHD.dto.CreateOrderRequest;
import dev.anhhoang.QTCSDLHD.dto.ProductResponse;
import dev.anhhoang.QTCSDLHD.dto.UserProfileResponse;

import java.util.List;

public interface CustomerService {
    UserProfileResponse addProductToCart(String customerId, AddToCartRequest request);

    UserProfileResponse removeProductFromCart(String customerId, RemoveFromCartRequest request);

    UserProfileResponse updateCartItemQuantity(String customerId, String productId, Integer quantity);

    List<ProductResponse> getCartProducts(String customerId);

    String createOrderFromCart(String customerId, CreateOrderRequest request);
}