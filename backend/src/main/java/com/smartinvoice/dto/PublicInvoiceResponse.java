package com.smartinvoice.dto;

import com.smartinvoice.enums.InvoiceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Read-only invoice view returned by the public, unauthenticated endpoint
 * ({@code GET /api/v1/public/invoices/{token}}). Deliberately a separate shape from
 * {@link InvoiceResponse} so we only ever expose what a client should see - no internal
 * user id, no client id, just who it's from/to and what's owed.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicInvoiceResponse {

    private String invoiceNumber;
    private InvoiceStatus status;

    private String issuerName;
    private String issuerEmail;

    private String clientName;

    private LocalDate issueDate;
    private LocalDate dueDate;

    private String currency;
    private BigDecimal subtotal;
    private BigDecimal taxRate;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private BigDecimal amountPaid;
    private BigDecimal balanceDue;

    private String notes;
    private List<InvoiceItemResponse> items;
}
