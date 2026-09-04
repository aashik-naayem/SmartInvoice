package com.smartinvoice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecurringInvoiceItemResponse {

    private Long id;
    private String description;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
}
