package dev.anhhoang.QTCSDLHD.controllers;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.anhhoang.QTCSDLHD.models.Product;
import dev.anhhoang.QTCSDLHD.models.Role;
import dev.anhhoang.QTCSDLHD.models.User;
import dev.anhhoang.QTCSDLHD.repositories.ProductRepository;
import dev.anhhoang.QTCSDLHD.repositories.UserRepository;

@RestController
@RequestMapping("/api/seller/products")
public class SellerProductController {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProductRepository productRepository;

    @PostMapping("/create")
    public ResponseEntity<?> createProduct(@RequestBody Product product, Principal principal) {
        Optional<User> userOpt = userRepository.findByEmail(principal.getName());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body("User not found");
        }
        User user = userOpt.get();
        boolean isSeller = user.getRoles() != null && user.getRoles().contains(Role.ROLE_SELLER);
        if (!isSeller || user.getSellerProfile() == null) {
            return ResponseEntity.status(403).body("Only sellers can create products");
        }
        product.setShopid(user.getSellerProfile().getShopId());
        product.setShopname(user.getSellerProfile().getShopName());
        Product saved = productRepository.save(product);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/mine")
    public ResponseEntity<?> getAllProducts(Principal principal) {
        Optional<User> userOpt = userRepository.findByEmail(principal.getName());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body("User not found");
        }
        User user = userOpt.get();
        boolean isSeller = user.getRoles() != null && user.getRoles().contains(Role.ROLE_SELLER);
        if (!isSeller || user.getSellerProfile() == null) {
            return ResponseEntity.status(403).body("Only sellers can view their products");
        }
        String shopId = user.getSellerProfile().getShopId();
        List<Product> products = productRepository.findByShopid(shopId);
        return ResponseEntity.ok(products);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable String id, @RequestBody Product updatedProduct,
            Principal principal) {
        Optional<User> userOpt = userRepository.findByEmail(principal.getName());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body("User not found");
        }
        User user = userOpt.get();
        boolean isSeller = user.getRoles() != null && user.getRoles().contains(Role.ROLE_SELLER);
        if (!isSeller || user.getSellerProfile() == null) {
            return ResponseEntity.status(403).body("Only sellers can update products");
        }
        String shopId = user.getSellerProfile().getShopId();
        Optional<Product> productOpt = productRepository.findById(id);
        if (productOpt.isEmpty()) {
            return ResponseEntity.status(404).body("Product not found");
        }
        Product product = productOpt.get();
        if (!shopId.equals(product.getShopid())) {
            return ResponseEntity.status(403).body("You can only update your own products");
        }
        // Cập nhật các trường
        product.setName(updatedProduct.getName());
        product.setDescription(updatedProduct.getDescription());
        product.setPrice(updatedProduct.getPrice());
        product.setImage_url(updatedProduct.getImage_url());
        product.setCategory(updatedProduct.getCategory());
        product.setStock(updatedProduct.getStock());
        Product saved = productRepository.save(product);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable String id, Principal principal) {
        Optional<User> userOpt = userRepository.findByEmail(principal.getName());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body("User not found");
        }
        User user = userOpt.get();
        boolean isSeller = user.getRoles() != null && user.getRoles().contains(Role.ROLE_SELLER);
        if (!isSeller || user.getSellerProfile() == null) {
            return ResponseEntity.status(403).body("Only sellers can delete products");
        }
        String shopId = user.getSellerProfile().getShopId();
        Optional<Product> productOpt = productRepository.findById(id);
        if (productOpt.isEmpty()) {
            return ResponseEntity.status(404).body("Product not found");
        }
        Product product = productOpt.get();
        if (!shopId.equals(product.getShopid())) {
            return ResponseEntity.status(403).body("You can only delete your own products");
        }
        productRepository.deleteById(id);
        return ResponseEntity.ok("Product deleted successfully");
    }
}
