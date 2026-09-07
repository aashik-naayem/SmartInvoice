package com.smartinvoice.repository;

import com.smartinvoice.entity.Invoice;
import com.smartinvoice.entity.User;
import com.smartinvoice.enums.InvoiceStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    List<Invoice> findByUser(User user);

    Optional<Invoice> findByIdAndUser(Long id, User user);

    boolean existsByInvoiceNumber(String invoiceNumber);

    List<Invoice> findByStatusIn(List<InvoiceStatus> statuses);

    List<Invoice> findByStatusAndDueDateBefore(InvoiceStatus status, LocalDate date);

    long countByUser(User user);

    @Query("SELECT i.status, COUNT(i) FROM Invoice i WHERE i.user = :user GROUP BY i.status")
    List<Object[]> countInvoicesByStatusForUser(@Param("user") User user);

    /**
     * Sum of (total - amount paid) across every non-{@code excludedStatus} invoice for the user.
     * Fully paid invoices naturally contribute ~0, so it's safe to sum across every remaining status.
     */
    @Query("SELECT COALESCE(SUM(i.totalAmount - COALESCE((SELECT SUM(p.amount) FROM Payment p WHERE p.invoice = i), 0)), 0) "
            + "FROM Invoice i WHERE i.user = :user AND i.status <> :excludedStatus")
    BigDecimal sumOutstandingBalanceForUser(@Param("user") User user, @Param("excludedStatus") InvoiceStatus excludedStatus);

    @Query("SELECT COALESCE(SUM(i.totalAmount - COALESCE((SELECT SUM(p.amount) FROM Payment p WHERE p.invoice = i), 0)), 0) "
            + "FROM Invoice i WHERE i.user = :user AND i.status = :status")
    BigDecimal sumBalanceForUserByStatus(@Param("user") User user, @Param("status") InvoiceStatus status);

    @Query("SELECT i.client, SUM(i.totalAmount) FROM Invoice i WHERE i.user = :user AND i.status <> :excludedStatus "
            + "GROUP BY i.client ORDER BY SUM(i.totalAmount) DESC")
    List<Object[]> topClientsByBilledForUser(@Param("user") User user, @Param("excludedStatus") InvoiceStatus excludedStatus, Pageable pageable);
}
