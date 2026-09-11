package com.smartinvoice.service;

import com.smartinvoice.dto.InvoiceItemRequest;
import com.smartinvoice.dto.InvoiceRequest;
import com.smartinvoice.dto.InvoiceResponse;
import com.smartinvoice.email.EmailService;
import com.smartinvoice.entity.Client;
import com.smartinvoice.entity.Invoice;
import com.smartinvoice.entity.User;
import com.smartinvoice.enums.InvoiceStatus;
import com.smartinvoice.exception.EmailDeliveryException;
import com.smartinvoice.pdf.InvoicePdfService;
import com.smartinvoice.repository.ClientRepository;
import com.smartinvoice.repository.InvoiceRepository;
import com.smartinvoice.repository.PaymentRepository;
import com.smartinvoice.repository.UserRepository;
import com.smartinvoice.security.SecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Focused on the two places InvoiceService is most likely to break silently: the totals math in
 * create() (wrong tax/subtotal math means wrong money on a real invoice), and the state
 * transitions in sendToClient() (an invoice should never end up marked SENT if the email never
 * actually went out - see EmailService's EmailDeliveryException contract).
 */
@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private InvoicePdfService invoicePdfService;
    @Mock
    private EmailService emailService;

    private InvoiceService invoiceService;
    private MockedStatic<SecurityUtils> securityUtils;

    private User owner;
    private Client client;

    @BeforeEach
    void setUp() {
        invoiceService = new InvoiceService(
                invoiceRepository, clientRepository, userRepository, paymentRepository,
                invoicePdfService, emailService);

        owner = User.builder().id(1L).fullName("Ashik Nayem").email("ashik@example.com").build();
        client = Client.builder().id(10L).user(owner).name("Acme Co").email("billing@acme.com").build();

        // getCurrentUser() reads the email off SecurityContextHolder via this static helper -
        // stub it here so every test doesn't need to fake a Spring SecurityContext.
        securityUtils = Mockito.mockStatic(SecurityUtils.class);
        securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn("ashik@example.com");
        when(userRepository.findByEmail("ashik@example.com")).thenReturn(Optional.of(owner));
    }

    @AfterEach
    void tearDown() {
        securityUtils.close();
    }

    @Test
    void create_calculatesSubtotalTaxAndTotalCorrectly() {
        InvoiceRequest request = new InvoiceRequest();
        request.setClientId(10L);
        request.setIssueDate(LocalDate.of(2026, 1, 1));
        request.setDueDate(LocalDate.of(2026, 1, 31));
        request.setCurrency("usd");
        request.setTaxRate(new BigDecimal("10"));
        request.setItems(List.of(
                item("Design work", "2", "150.00"),   // 300.00
                item("Hosting", "1", "49.99")          // 49.99
        ));

        when(clientRepository.findByIdAndUser(10L, owner)).thenReturn(Optional.of(client));
        when(invoiceRepository.existsByInvoiceNumber(any())).thenReturn(false);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentRepository.sumAmountByInvoice(any())).thenReturn(BigDecimal.ZERO);

        InvoiceResponse response = invoiceService.create(request);

        // subtotal = 300.00 + 49.99 = 349.99 ; tax = 349.99 * 10% = 35.00 (half-up) ; total = 384.99
        assertThat(response.getSubtotal()).isEqualByComparingTo("349.99");
        assertThat(response.getTaxAmount()).isEqualByComparingTo("35.00");
        assertThat(response.getTotalAmount()).isEqualByComparingTo("384.99");
        assertThat(response.getCurrency()).isEqualTo("USD"); // currency is always uppercased
        assertThat(response.getStatus()).isEqualTo(InvoiceStatus.DRAFT);
    }

    @Test
    void create_rejectsAClientThatDoesNotBelongToTheCurrentUser() {
        InvoiceRequest request = new InvoiceRequest();
        request.setClientId(999L);
        request.setIssueDate(LocalDate.now());
        request.setDueDate(LocalDate.now().plusDays(30));
        request.setCurrency("USD");
        request.setTaxRate(BigDecimal.ZERO);
        request.setItems(List.of(item("Item", "1", "10.00")));

        when(clientRepository.findByIdAndUser(999L, owner)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.create(request))
                .isInstanceOf(com.smartinvoice.exception.ResourceNotFoundException.class);
    }

    @Test
    void sendToClient_generatesTokenAndMarksSentOnSuccess() {
        Invoice invoice = Invoice.builder()
                .id(5L).user(owner).client(client)
                .invoiceNumber("INV-1").status(InvoiceStatus.DRAFT)
                .totalAmount(new BigDecimal("100.00"))
                .items(new java.util.ArrayList<>())
                .build();

        when(invoiceRepository.findByIdAndUser(5L, owner)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(invoicePdfService.renderInvoicePdf(any())).thenReturn(new byte[] {1, 2, 3});
        when(paymentRepository.sumAmountByInvoice(any())).thenReturn(BigDecimal.ZERO);

        InvoiceResponse response = invoiceService.sendToClient(5L);

        assertThat(response.getStatus()).isEqualTo(InvoiceStatus.SENT);
        assertThat(response.getPublicToken()).isNotBlank();
        assertThat(response.getSentAt()).isNotNull();
    }

    @Test
    void sendToClient_leavesInvoiceUntouchedWhenEmailDeliveryFails() {
        // This documents the contract: sendToClient itself doesn't roll back a failed email
        // (that's @Transactional's job at the Spring proxy level, which plain Mockito unit tests
        // can't exercise) - but it must propagate the failure rather than swallow it, so the
        // caller/controller sees a real error instead of a false "sent" response.
        Invoice invoice = Invoice.builder()
                .id(5L).user(owner).client(client)
                .invoiceNumber("INV-1").status(InvoiceStatus.DRAFT)
                .totalAmount(new BigDecimal("100.00"))
                .items(new java.util.ArrayList<>())
                .build();

        when(invoiceRepository.findByIdAndUser(5L, owner)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(invoicePdfService.renderInvoicePdf(any())).thenReturn(new byte[] {1, 2, 3});
        Mockito.doThrow(new EmailDeliveryException("SMTP auth failed"))
                .when(emailService).sendInvoice(any(), any());

        assertThatThrownBy(() -> invoiceService.sendToClient(5L))
                .isInstanceOf(EmailDeliveryException.class);
    }

    private InvoiceItemRequest item(String description, String quantity, String unitPrice) {
        return new InvoiceItemRequest(description, new BigDecimal(quantity), new BigDecimal(unitPrice));
    }
}
