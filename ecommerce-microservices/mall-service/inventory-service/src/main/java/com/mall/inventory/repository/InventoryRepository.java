package com.mall.inventory.repository;

import com.mall.inventory.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByProductId(Long productId);

    @Modifying
    @Query("UPDATE Inventory i SET i.stock = i.stock - :quantity WHERE i.productId = :productId AND i.stock >= :quantity")
    int deductStock(Long productId, Integer quantity);

    @Modifying
    @Query("UPDATE Inventory i SET i.stock = i.stock + :quantity WHERE i.productId = :productId")
    int restoreStock(Long productId, Integer quantity);
}
