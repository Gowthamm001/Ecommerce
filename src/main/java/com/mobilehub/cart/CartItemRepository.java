package com.mobilehub.cart;

import com.mobilehub.account.Account;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByAccountIdOrderByIdAsc(String accountId);
    Optional<CartItem> findByIdAndAccount(Long id, Account account);
    Optional<CartItem> findByAccountAndProduct_IdAndVariantRamAndVariantStorageAndColor(
            Account account, String productId, String variantRam, String variantStorage, String color);
    boolean existsByProduct_Id(String productId);
    void deleteByIdAndAccount(Long id, Account account);
    void deleteByAccount(Account account);
}
