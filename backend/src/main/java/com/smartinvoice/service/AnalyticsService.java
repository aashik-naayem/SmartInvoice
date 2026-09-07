package com.smartinvoice.service;

import com.smartinvoice.dto.ClientRevenueSummary;
import com.smartinvoice.dto.DashboardResponse;
import com.smartinvoice.dto.MonthlyRevenuePoint;
import com.smartinvoice.dto.RecentPaymentSummary;
import com.smartinvoice.entity.Client;
import com.smartinvoice.entity.Payment;
import com.smartinvoice.entity.User;
import com.smartinvoice.enums.InvoiceStatus;
import com.smartinvoice.exception.ResourceNotFoundException;
import com.smartinvoice.repository.ClientRepository;
import com.smartinvoice.repository.InvoiceRepository;
import com.smartinvoice.repository.PaymentRepository;
import com.smartinvoice.repository.UserRepository;
import com.smartinvoice.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the business-analytics dashboard for the current user. Everything is scoped to the
 * authenticated user (same ownership model as the rest of the app) and computed with aggregate
 * queries rather than loading every invoice/payment into memory.
 */
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private static final int MONTHLY_TREND_MONTHS = 12;
    private static final int TOP_CLIENTS_LIMIT = 5;

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {
        User currentUser = getCurrentUser();

        return DashboardResponse.builder()
                .totalRevenue(paymentRepository.sumAllAmountForUser(currentUser))
                .outstandingBalance(invoiceRepository.sumOutstandingBalanceForUser(currentUser, InvoiceStatus.CANCELLED))
                .overdueBalance(invoiceRepository.sumBalanceForUserByStatus(currentUser, InvoiceStatus.OVERDUE))
                .totalClients(clientRepository.countByUser(currentUser))
                .totalInvoices(invoiceRepository.countByUser(currentUser))
                .invoiceCountsByStatus(buildStatusCounts(currentUser))
                .monthlyRevenue(buildMonthlyRevenueTrend(currentUser))
                .topClients(buildTopClients(currentUser))
                .recentPayments(buildRecentPayments(currentUser))
                .build();
    }

    private Map<InvoiceStatus, Long> buildStatusCounts(User user) {
        Map<InvoiceStatus, Long> counts = new EnumMap<>(InvoiceStatus.class);
        for (InvoiceStatus status : InvoiceStatus.values()) {
            counts.put(status, 0L);
        }
        for (Object[] row : invoiceRepository.countInvoicesByStatusForUser(user)) {
            counts.put((InvoiceStatus) row[0], (Long) row[1]);
        }
        return counts;
    }

    private List<MonthlyRevenuePoint> buildMonthlyRevenueTrend(User user) {
        YearMonth startMonth = YearMonth.now().minusMonths(MONTHLY_TREND_MONTHS - 1L);
        LocalDate since = startMonth.atDay(1);

        Map<YearMonth, BigDecimal> totalsByMonth = new LinkedHashMap<>();
        for (int i = 0; i < MONTHLY_TREND_MONTHS; i++) {
            totalsByMonth.put(startMonth.plusMonths(i), BigDecimal.ZERO);
        }

        List<Payment> payments = paymentRepository
                .findByInvoice_UserAndPaymentDateGreaterThanEqualOrderByPaymentDateAsc(user, since);
        for (Payment payment : payments) {
            YearMonth month = YearMonth.from(payment.getPaymentDate());
            totalsByMonth.merge(month, payment.getAmount(), BigDecimal::add);
        }

        return totalsByMonth.entrySet().stream()
                .map(entry -> MonthlyRevenuePoint.builder()
                        .month(entry.getKey().toString())
                        .amount(entry.getValue())
                        .build())
                .toList();
    }

    private List<ClientRevenueSummary> buildTopClients(User user) {
        List<Object[]> rows = invoiceRepository.topClientsByBilledForUser(
                user, InvoiceStatus.CANCELLED, PageRequest.of(0, TOP_CLIENTS_LIMIT));

        return rows.stream()
                .map(row -> {
                    Client client = (Client) row[0];
                    BigDecimal totalBilled = (BigDecimal) row[1];
                    return ClientRevenueSummary.builder()
                            .clientId(client.getId())
                            .clientName(client.getName())
                            .totalBilled(totalBilled)
                            .build();
                })
                .toList();
    }

    private List<RecentPaymentSummary> buildRecentPayments(User user) {
        return paymentRepository.findTop5ByInvoice_UserOrderByPaymentDateDescCreatedAtDesc(user).stream()
                .map(payment -> RecentPaymentSummary.builder()
                        .paymentId(payment.getId())
                        .invoiceId(payment.getInvoice().getId())
                        .invoiceNumber(payment.getInvoice().getInvoiceNumber())
                        .clientName(payment.getInvoice().getClient().getName())
                        .amount(payment.getAmount())
                        .paymentDate(payment.getPaymentDate())
                        .build())
                .toList();
    }

    private User getCurrentUser() {
        String email = SecurityUtils.getCurrentUserEmail();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }
}
