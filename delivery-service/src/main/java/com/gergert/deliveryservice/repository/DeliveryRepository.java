package com.gergert.deliveryservice.repository;

import com.gergert.deliveryservice.entity.Delivery;
import com.gergert.deliveryservice.entity.DeliveryStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select d
        from Delivery d
        where d.orderId = :orderId
    """)
    Optional<Delivery> findByOrderIdForUpdate(Long orderId);
    Optional<Delivery> findByOrderId(Long orderId);

    List<Delivery> findAllByCourier_UserId(Long userId);
    Optional<Delivery> findFirstByCourier_UserIdAndDeliveryStatusIn(Long courierUserId, Set<DeliveryStatus> status);
    List<Delivery> findAllByDeliveryStatus(DeliveryStatus status);

    Long countByCourier_UserIdAndCompletedAtBetween(Long courierUserId, LocalDateTime start, LocalDateTime end);
}
