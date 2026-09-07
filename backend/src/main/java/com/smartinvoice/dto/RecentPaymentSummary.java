package com.smartinvoice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecentPaymentSummary {

    private Long paymentId;
    private Long invoiceId;
    private String invoiceNumber;
    private String clientName;
    private BigDecimal amount;
    private LocalDate paymentDate;
}
