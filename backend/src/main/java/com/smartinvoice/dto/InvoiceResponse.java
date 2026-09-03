package com.smartinvoice.dto;

import com.smartinvoice.enums.InvoiceStatus;
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
public class InvoiceResponse {

    private Long id;
    private String invoiceNumber;
    private Long clientId;
    private String clientName;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private InvoiceStatus status;
    private String currency;
    private BigDecimal subtotal;
    private BigDecimal taxRate;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private String notes;
    private List<InvoiceItemResponse> items;
    private LocalDateTime createdAt;
}
