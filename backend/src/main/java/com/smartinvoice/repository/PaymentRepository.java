package com.smartinvoice.repository;

import com.smartinvoice.entity.Invoice;
import com.smartinvoice.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByInvoiceOrderByPaymentDateDescCreatedAtDesc(Invoice invoice);

    Optional<Payment> findByIdAndInvoice(Long id, Invoice invoice);

    void deleteByInvoice(Invoice invoice);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.invoice = :invoice")
    BigDecimal sumAmountByInvoice(@Param("invoice") Invoice invoice);
}
