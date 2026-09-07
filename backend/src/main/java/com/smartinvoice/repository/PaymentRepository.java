package com.smartinvoice.repository;

import com.smartinvoice.entity.Invoice;
import com.smartinvoice.entity.Payment;
import com.smartinvoice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByInvoiceOrderByPaymentDateDescCreatedAtDesc(Invoice invoice);

    Optional<Payment> findByIdAndInvoice(Long id, Invoice invoice);

    void deleteByInvoice(Invoice invoice);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.invoice = :invoice")
    BigDecimal sumAmountByInvoice(@Param("invoice") Invoice invoice);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.invoice.user = :user")
    BigDecimal sumAllAmountForUser(@Param("user") User user);

    List<Payment> findByInvoice_UserAndPaymentDateGreaterThanEqualOrderByPaymentDateAsc(User user, LocalDate since);

    List<Payment> findTop5ByInvoice_UserOrderByPaymentDateDescCreatedAtDesc(User user);
}
