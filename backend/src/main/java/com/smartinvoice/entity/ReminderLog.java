package com.smartinvoice.entity;

import com.smartinvoice.enums.ReminderType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Records that a reminder email of a given {@link ReminderType} was sent for an invoice, so the
 * scheduler doesn't email the client twice for the same one-off reminder (upcoming / due-today),
 * and can space out repeated overdue reminders instead of sending one every run.
 */
@Entity
@Table(name = "reminder_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReminderLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReminderType reminderType;

    @Column(nullable = false)
    private LocalDateTime sentAt;
}
