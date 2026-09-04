package com.smartinvoice.dto;

import com.smartinvoice.enums.RecurrenceFrequency;
import com.smartinvoice.enums.RecurringInvoiceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecurringInvoiceResponse {

    private Long id;
    private Long clientId;
    private String clientName;
    private RecurrenceFrequency frequency;
    private LocalDate startDate;
    private LocalDate nextRunDate;
    private LocalDate endDate;
    private Integer daysDueAfterIssue;
    private String currency;
    private BigDecimal taxRate;
    private String notes;
    private boolean autoSend;
    private RecurringInvoiceStatus status;
    private Integer occurrencesGenerated;
    private List<RecurringInvoiceItemResponse> items;
    private LocalDateTime createdAt;
}
