package com.smartinvoice.dto;

import com.smartinvoice.enums.InvoiceStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceStatusUpdateRequest {

    @NotNull(message = "Status is required")
    private InvoiceStatus status;
}
