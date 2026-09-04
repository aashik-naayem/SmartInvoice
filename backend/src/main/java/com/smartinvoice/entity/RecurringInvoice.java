package com.smartinvoice.entity;

import com.smartinvoice.enums.RecurrenceFrequency;
import com.smartinvoice.enums.RecurringInvoiceStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A recurring billing schedule for a client. Acts as a template: the scheduler generates a
 * concrete {@link Invoice} from it every time {@code nextRunDate} is reached, then advances
 * the schedule based on {@code frequency} until {@code endDate} (if any) is passed.
 */
@Entity
@Table(name = "recurring_invoices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecurringInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecurrenceFrequency frequency;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate nextRunDate;

    private LocalDate endDate;

    @Column(nullable = false)
    private Integer daysDueAfterIssue;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal taxRate;

    @Column(length = 500)
    private String notes;

    @Column(nullable = false)
    private boolean autoSend;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecurringInvoiceStatus status;

    @Column(nullable = false)
    @Builder.Default
    private Integer occurrencesGenerated = 0;

    @OneToMany(mappedBy = "recurringInvoice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<RecurringInvoiceItem> items = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public void addItem(RecurringInvoiceItem item) {
        items.add(item);
        item.setRecurringInvoice(this);
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
