package com.smartinvoice.repository;

import com.smartinvoice.entity.Invoice;
import com.smartinvoice.entity.ReminderLog;
import com.smartinvoice.enums.ReminderType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReminderLogRepository extends JpaRepository<ReminderLog, Long> {

    boolean existsByInvoiceAndReminderType(Invoice invoice, ReminderType reminderType);

    Optional<ReminderLog> findTopByInvoiceAndReminderTypeOrderBySentAtDesc(Invoice invoice, ReminderType reminderType);
}
