package dev.anhhoang.QTCSDLHD.controllers;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.anhhoang.QTCSDLHD.models.Product;
import dev.anhhoang.QTCSDLHD.models.Role;
import dev.anhhoang.QTCSDLHD.models.User;
import dev.anhhoang.QTCSDLHD.models.Voucher;
import dev.anhhoang.QTCSDLHD.repositories.ProductRepository;
import dev.anhhoang.QTCSDLHD.repositories.UserRepository;
import dev.anhhoang.QTCSDLHD.services.VoucherService;

@RestController
@RequestMapping("/api/vouchers")
public class VoucherController {
    @Autowired
    private VoucherService voucherService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProductRepository productRepository;

    // Tạo voucher chỉ cho seller, tự động lấy shopId từ sellerProfile
    @PostMapping
    public ResponseEntity<?> createVoucher(@RequestBody Voucher voucher, Principal principal) {
        Optional<User> userOpt = userRepository.findByEmail(principal.getName());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body("User not found");
        }
        User user = userOpt.get();
        boolean isSeller = user.getRoles() != null && user.getRoles().contains(Role.ROLE_SELLER);
        if (!isSeller || user.getSellerProfile() == null) {
            return ResponseEntity.status(403).body("Only sellers can create vouchers");
        }
        voucher.setShopId(user.getSellerProfile().getShopId());
        voucher.setType(Voucher.VoucherType.SHOP);
        return ResponseEntity.ok(voucherService.createVoucher(voucher));
    }

    // Lấy voucher theo shopId (chỉ lấy của shop)
    @GetMapping("/shop/{shopId}")
    public ResponseEntity<List<Voucher>> getVouchersByShop(@PathVariable String shopId) {
        return ResponseEntity.ok(voucherService.getVouchersByShopId(shopId));
    }

    // Lấy voucher theo shopId của user đang đăng nhập (không nhận shopId từ FE)
    @GetMapping("/shop")
    public ResponseEntity<?> getVouchersByCurrentShop(Principal principal) {
        Optional<User> userOpt = userRepository.findByEmail(principal.getName());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body("User not found");
        }
        User user = userOpt.get();
        boolean isSeller = user.getRoles() != null && user.getRoles().contains(Role.ROLE_SELLER);
        if (!isSeller || user.getSellerProfile() == null) {
            return ResponseEntity.status(403).body("Only sellers can view their vouchers");
        }
        String shopId = user.getSellerProfile().getShopId();
        return ResponseEntity.ok(voucherService.getVouchersByShopId(shopId));
    }

    // Cập nhật voucher (chỉ cho seller, kiểm tra shopId)
    @PutMapping("/{id}")
    public ResponseEntity<?> updateVoucher(@PathVariable String id, @RequestBody Voucher voucher, Principal principal) {
        Optional<User> userOpt = userRepository.findByEmail(principal.getName());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body("User not found");
        }
        User user = userOpt.get();
        boolean isSeller = user.getRoles() != null && user.getRoles().contains(Role.ROLE_SELLER);
        if (!isSeller || user.getSellerProfile() == null) {
            return ResponseEntity.status(403).body("Only sellers can update vouchers");
        }
        // Chỉ cho phép sửa voucher của shop mình
        Voucher old = voucherService.getVoucherById(id);
        if (old == null || !user.getSellerProfile().getShopId().equals(old.getShopId())) {
            return ResponseEntity.status(403).body("Not allowed");
        }
        voucher.setShopId(user.getSellerProfile().getShopId());
        voucher.setType(Voucher.VoucherType.SHOP);
        return ResponseEntity.ok(voucherService.updateVoucher(id, voucher));
    }

    // Xóa voucher (chỉ cho seller, kiểm tra shopId)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteVoucher(@PathVariable String id, Principal principal) {
        Optional<User> userOpt = userRepository.findByEmail(principal.getName());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body("User not found");
        }
        User user = userOpt.get();
        boolean isSeller = user.getRoles() != null && user.getRoles().contains(Role.ROLE_SELLER);
        if (!isSeller || user.getSellerProfile() == null) {
            return ResponseEntity.status(403).body("Only sellers can delete vouchers");
        }
        Voucher old = voucherService.getVoucherById(id);
        if (old == null || !user.getSellerProfile().getShopId().equals(old.getShopId())) {
            return ResponseEntity.status(403).body("Not allowed");
        }
        voucherService.deleteVoucher(id);
        return ResponseEntity.ok().build();
    }

    // Hủy áp dụng voucher cho 1 sản phẩm (xóa productId khỏi voucher)
    @PatchMapping("/remove-product")
    public ResponseEntity<?> removeProductFromVoucher(@RequestBody Map<String, String> payload, Principal principal) {
        String voucherId = payload.get("voucherId");
        String productId = payload.get("productId");
        Optional<User> userOpt = userRepository.findByEmail(principal.getName());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body("User not found");
        }
        User user = userOpt.get();
        boolean isSeller = user.getRoles() != null && user.getRoles().contains(Role.ROLE_SELLER);
        if (!isSeller || user.getSellerProfile() == null) {
            return ResponseEntity.status(403).body("Only sellers can update vouchers");
        }
        Voucher voucher = voucherService.getVoucherById(voucherId);
        if (voucher == null || !user.getSellerProfile().getShopId().equals(voucher.getShopId())) {
            return ResponseEntity.status(403).body("Not allowed");
        }
        boolean result = voucherService.removeProductFromVoucher(voucherId, productId);
        if (result) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(400).body("Remove product from voucher failed");
        }
    }

    // Lấy voucher áp dụng cho một sản phẩm cụ thể
    @GetMapping("/product/{productId}")
    public ResponseEntity<?> getVouchersForProduct(@PathVariable String productId) {
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isEmpty()) {
            return ResponseEntity.status(404).body("Product not found");
        }
        Product product = productOpt.get();
        return ResponseEntity.ok(voucherService.getApplicableVouchersForProduct(productId,
                new java.math.BigDecimal(product.getPrice())));
    }
}
