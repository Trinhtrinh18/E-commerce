package dev.anhhoang.QTCSDLHD.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dev.anhhoang.QTCSDLHD.models.Voucher;
import dev.anhhoang.QTCSDLHD.repositories.VoucherRepository;

@Service
public class VoucherService {
    @Autowired
    private VoucherRepository voucherRepository;

    public Voucher createVoucher(Voucher voucher) {
        return voucherRepository.save(voucher);
    }

    public List<Voucher> getVouchersByShopId(String shopId) {
        return voucherRepository.findByShopId(shopId);
    }

    public List<Voucher> getPlatformVouchers() {
        return voucherRepository.findByType(Voucher.VoucherType.PLATFORM);
    }

    public Optional<Voucher> getVoucherByCode(String code) {
        return voucherRepository.findByCode(code);
    }

    public List<Voucher> getApplicableVouchersForProduct(String productId, BigDecimal productPrice) {
        LocalDateTime now = LocalDateTime.now();
        List<Voucher> productVouchers = voucherRepository.findByProductIdsContaining(productId);
        
        // Only filter out expired or not yet active vouchers
        // minOrderValue will be checked when applying voucher with actual cart total
        return productVouchers.stream()
                .filter(voucher -> ((voucher.getStartDate() == null || !now.isBefore(voucher.getStartDate())) &&
                        (voucher.getEndDate() == null || !now.isAfter(voucher.getEndDate()))))
                .collect(java.util.stream.Collectors.toList());
    }

    public boolean isVoucherValid(Voucher voucher, BigDecimal orderTotal) {
        LocalDateTime now = LocalDateTime.now();
        return (voucher.getStartDate() == null || !now.isBefore(voucher.getStartDate())) &&
                (voucher.getEndDate() == null || !now.isAfter(voucher.getEndDate())) &&
                (orderTotal.compareTo(voucher.getMinOrderValue()) >= 0);
    }

    public BigDecimal calculateDiscount(Voucher voucher, BigDecimal orderTotal) {
        if (voucher.getDiscountType() == Voucher.DiscountType.PERCENTAGE) {
            return orderTotal.multiply(voucher.getDiscountValue()).divide(BigDecimal.valueOf(100));
        } else {
            return voucher.getDiscountValue().min(orderTotal);
        }
    }

    public Voucher updateVoucher(String id, Voucher updatedVoucher) {
        Optional<Voucher> optionalVoucher = voucherRepository.findById(id);
        if (optionalVoucher.isPresent()) {
            Voucher voucher = optionalVoucher.get();
            voucher.setCode(updatedVoucher.getCode());
            voucher.setType(updatedVoucher.getType());
            voucher.setDiscountType(updatedVoucher.getDiscountType());
            voucher.setDiscountValue(updatedVoucher.getDiscountValue());
            voucher.setMinOrderValue(updatedVoucher.getMinOrderValue());
            voucher.setStartDate(updatedVoucher.getStartDate());
            voucher.setEndDate(updatedVoucher.getEndDate());
            voucher.setShopId(updatedVoucher.getShopId());
            voucher.setProductIds(updatedVoucher.getProductIds());
            return voucherRepository.save(voucher);
        }
        return null;
    }

    public boolean deleteVoucher(String id) {
        if (voucherRepository.existsById(id)) {
            voucherRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public Voucher getVoucherById(String id) {
        return voucherRepository.findById(id).orElse(null);
    }

    public boolean removeProductFromVoucher(String voucherId, String productId) {
        Optional<Voucher> optionalVoucher = voucherRepository.findById(voucherId);
        if (optionalVoucher.isPresent()) {
            Voucher voucher = optionalVoucher.get();
            if (voucher.getProductIds() != null && voucher.getProductIds().remove(productId)) {
                voucherRepository.save(voucher);
                return true;
            }
        }
        return false;
    }
}
