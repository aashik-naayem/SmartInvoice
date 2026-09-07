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
public class MonthlyRevenuePoint {

    /** ISO year-month, e.g. "2026-01". */
    private String month;
    private BigDecimal amount;
}
