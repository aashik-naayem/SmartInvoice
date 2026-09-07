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
public class ClientRevenueSummary {

    private Long clientId;
    private String clientName;
    private BigDecimal totalBilled;
}
