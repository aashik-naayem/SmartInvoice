package com.smartinvoice.dto;

import com.smartinvoice.enums.InvoiceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardResponse {

    /** All-time sum of recorded payments across every invoice. */
    private BigDecimal totalRevenue;

    /** Sum of (total - paid) across every non-cancelled invoice. */
    private BigDecimal outstandingBalance;

    /** Sum of (total - paid) across invoices currently OVERDUE. */
    private BigDecimal overdueBalance;

    private long totalClients;
    private long totalInvoices;

    private Map<InvoiceStatus, Long> invoiceCountsByStatus;

    /** Payments received per calendar month, oldest to newest, for the trailing 12 months. */
    private List<MonthlyRevenuePoint> monthlyRevenue;

    /** Top 5 clients by total amount billed (excluding cancelled invoices). */
    private List<ClientRevenueSummary> topClients;

    /** Most recent 5 payments received, across all invoices. */
    private List<RecentPaymentSummary> recentPayments;
}
