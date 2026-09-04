package com.smartinvoice.dto;

import com.smartinvoice.enums.RecurrenceFrequency;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecurringInvoiceRequest {

    @NotNull(message = "Client id is required")
    private Long clientId;

    @NotNull(message = "Frequency is required")
    private RecurrenceFrequency frequency;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    /** Optional. When set, the schedule stops generating invoices once its next run would fall after this date. */
    private LocalDate endDate;

    @NotNull(message = "Days until due is required")
    @Min(value = 0, message = "Days until due cannot be negative")
    private Integer daysDueAfterIssue;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 10, message = "Currency should be a valid code, e.g. USD")
    private String currency;

    @NotNull(message = "Tax rate is required")
    @DecimalMin(value = "0.0", message = "Tax rate cannot be negative")
    @DecimalMax(value = "100.0", message = "Tax rate cannot exceed 100")
    private BigDecimal taxRate;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;

    /** When true, generated invoices are created as SENT rather than DRAFT. */
    private boolean autoSend;

    @NotEmpty(message = "Recurring invoice must contain at least one item")
    @Valid
    private List<RecurringInvoiceItemRequest> items;
}
