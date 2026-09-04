package com.smartinvoice.repository;

import com.smartinvoice.entity.RecurringInvoice;
import com.smartinvoice.entity.User;
import com.smartinvoice.enums.RecurringInvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RecurringInvoiceRepository extends JpaRepository<RecurringInvoice, Long> {

    List<RecurringInvoice> findByUser(User user);

    Optional<RecurringInvoice> findByIdAndUser(Long id, User user);

    List<RecurringInvoice> findByStatusAndNextRunDateLessThanEqual(RecurringInvoiceStatus status, LocalDate date);
}
