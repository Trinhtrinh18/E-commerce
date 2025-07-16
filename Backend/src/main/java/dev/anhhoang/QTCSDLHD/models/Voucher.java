package dev.anhhoang.QTCSDLHD.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "vouchers")
public class Voucher {
    @Id
    private String id;

    private String code;

    private VoucherType type; // SHOP hoặc PLATFORM

    private DiscountType discountType; // PERCENTAGE hoặc FIXED

    private BigDecimal discountValue;

    private BigDecimal minOrderValue;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    // Nếu là voucher của shop thì lưu shopId, nếu là platform thì null
    private String shopId;

    // Danh sách id sản phẩm áp dụng voucher này (nếu null hoặc rỗng thì áp dụng cho toàn bộ shop)
    private java.util.List<String> productIds;

    // Getter, Setter
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public VoucherType getType() {
        return type;
    }

    public void setType(VoucherType type) {
        this.type = type;
    }

    public DiscountType getDiscountType() {
        return discountType;
    }

    public void setDiscountType(DiscountType discountType) {
        this.discountType = discountType;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(BigDecimal discountValue) {
        this.discountValue = discountValue;
    }

    public BigDecimal getMinOrderValue() {
        return minOrderValue;
    }

    public void setMinOrderValue(BigDecimal minOrderValue) {
        this.minOrderValue = minOrderValue;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public String getShopId() {
        return shopId;
    }

    public void setShopId(String shopId) {
        this.shopId = shopId;
    }

    public java.util.List<String> getProductIds() {
        return productIds;
    }

    public void setProductIds(java.util.List<String> productIds) {
        this.productIds = productIds;
    }

    public enum VoucherType {
        SHOP, PLATFORM
    }

    public enum DiscountType {
        PERCENTAGE, FIXED
    }
}
