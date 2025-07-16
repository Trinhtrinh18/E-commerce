package dev.anhhoang.QTCSDLHD.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import dev.anhhoang.QTCSDLHD.models.Voucher;

@Repository
public interface VoucherRepository extends MongoRepository<Voucher, String> {
    Optional<Voucher> findByCode(String code);

    List<Voucher> findByShopId(String shopId);

    List<Voucher> findByType(Voucher.VoucherType type);

    List<Voucher> findByProductIdsContaining(String productId);
}
