package app.subscriptions;

import app.subscriptions.domain.catalogs.Customer;
import app.subscriptions.domain.catalogs.Tariff;
import app.subscriptions.domain.documents.Payment;
import app.subscriptions.domain.documents.Subscription;
import app.subscriptions.domain.documents.SubscriptionLine;
import app.subscriptions.domain.enums.CustomerStatus;
import app.subscriptions.domain.enums.PaymentMethod;
import app.subscriptions.domain.enums.SubscriptionStatus;
import app.subscriptions.domain.registers.CustomerAccount;
import app.subscriptions.domain.registers.TariffRevenue;
import app.subscriptions.jobs.SubscriptionStatusJob;
import app.subscriptions.repositories.CustomerAccountRepository;
import app.subscriptions.repositories.CustomerRepository;
import app.subscriptions.repositories.PaymentRepository;
import app.subscriptions.repositories.SubscriptionRepository;
import app.subscriptions.repositories.TariffRepository;
import app.subscriptions.repositories.TariffRevenueRepository;
import su.onno.posting.PostingService;
import su.onno.types.Ref;
import su.onno.validation.ValidationException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:subscription_tests;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "app.demo-seed=false",
        "org.jobrunr.background-job-server.enabled=false",
        "org.jobrunr.dashboard.enabled=false",
        "onno.auth.session.remember-me.allow-ephemeral-key=true"
})
class BillingIntegrationTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired CustomerRepository customers;
    @Autowired TariffRepository tariffs;
    @Autowired PaymentRepository payments;
    @Autowired SubscriptionRepository subscriptions;
    @Autowired CustomerAccountRepository accounts;
    @Autowired TariffRevenueRepository revenue;
    @Autowired PostingService posting;
    @Autowired SubscriptionStatusJob statusJob;

    @Test
    @DisplayName("BR-CLIENT: нельзя сохранить подписку без клиента")
    void subscriptionRequiresCustomer() {
        Tariff tariff = yearlyTariff();
        Subscription subscription = new Subscription();
        subscription.getLines().add(line(tariff, 1));

        assertThatThrownBy(() -> subscriptions.save(subscription))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).fieldErrors())
                        .containsKey("customer"));
    }

    @Test
    @DisplayName("BR-CLIENT: нельзя сохранить платёж без клиента")
    void paymentRequiresCustomer() {
        Payment payment = new Payment();
        payment.setAmount(new BigDecimal("100.00"));
        payment.setMethod(PaymentMethod.CARD);

        assertThatThrownBy(() -> payments.save(payment))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).fieldErrors())
                        .containsKey("customer"));
    }

    @Test
    @DisplayName("BR-LINES: нельзя сохранить подписку без строк")
    void subscriptionRequiresLines() {
        Subscription subscription = new Subscription();
        subscription.setCustomer(Ref.of(Customer.class, customer().getId()));

        assertThatThrownBy(() -> subscriptions.save(subscription))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("строк");
    }

    @Test
    @DisplayName("BR-PERIODS: число периодов должно быть больше нуля")
    void periodsMustBePositive() {
        Customer customer = customer();
        Tariff tariff = yearlyTariff();
        Subscription subscription = new Subscription();
        subscription.setCustomer(Ref.of(Customer.class, customer.getId()));
        subscription.getLines().add(line(tariff, 0));

        assertThatThrownBy(() -> subscriptions.save(subscription))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("периодов");
    }

    @Test
    @DisplayName("BR-TARIFF: недоступный тариф нельзя подключить")
    void unavailableTariffIsRejected() {
        Customer customer = customer();
        Tariff tariff = tariff("Архив", "100.00", 7, false);
        Subscription subscription = new Subscription();
        subscription.setCustomer(Ref.of(Customer.class, customer.getId()));
        subscription.getLines().add(line(tariff, 1));

        assertThatThrownBy(() -> subscriptions.save(subscription))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("доступен");
    }

    @Test
    @DisplayName("BR-FILL: дата документа и дата начала заполняются при создании")
    void fillingSetsDocumentAndStartDates() {
        Customer customer = customer();
        Tariff tariff = yearlyTariff();
        Subscription subscription = new Subscription();
        subscription.setCustomer(Ref.of(Customer.class, customer.getId()));
        subscription.getLines().add(line(tariff, 1));

        Subscription saved = subscriptions.save(subscription);

        assertThat(saved.getDate()).isNotNull();
        assertThat(saved.getStartDate()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(SubscriptionStatus.DRAFT);
    }

    @Test
    @DisplayName("BR-PRICE: цена строки берётся из тарифа, сумма и итог пересчитываются")
    void linePriceComesFromTariff() {
        Customer customer = customer();
        Tariff tariff = tariff("Про", "1500.00", 30, true);
        Subscription subscription = new Subscription();
        subscription.setCustomer(Ref.of(Customer.class, customer.getId()));
        subscription.getLines().add(line(tariff, 2));

        Subscription saved = subscriptions.save(subscription);
        SubscriptionLine savedLine = saved.getLines().getFirst();

        assertThat(savedLine.getPrice()).isEqualByComparingTo("1500.00");
        assertThat(savedLine.getAmount()).isEqualByComparingTo("3000.00");
        assertThat(saved.getTotal()).isEqualByComparingTo("3000.00");
    }

    @Test
    @DisplayName("BR-ENDDATE: окончание = начало + max срок среди строк (365 и 30 → 365)")
    void endDateUsesMaxLineDuration() {
        Customer customer = customer();
        Tariff yearly = tariff("Год", "9900.00", 365, true);
        Tariff monthly = tariff("Месяц", "990.00", 30, true);
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 10, 0);

        Subscription subscription = new Subscription();
        subscription.setCustomer(Ref.of(Customer.class, customer.getId()));
        subscription.setStartDate(start);
        subscription.getLines().add(line(yearly, 1));
        subscription.getLines().add(line(monthly, 1));

        Subscription saved = subscriptions.save(subscription);

        assertThat(saved.getEndDate()).isEqualTo(start.plusDays(365));
    }

    @Test
    @DisplayName("BR-PAY: проведение платежа пополняет лицевой счёт")
    void postingPaymentCreditsAccount() {
        Customer customer = customer();
        Payment payment = payment(customer, "5000.00");
        payments.save(payment);
        posting.post(payment);

        assertThat(balance(customer)).isEqualByComparingTo("5000.00");
    }

    @Test
    @DisplayName("BR-SUB: проведение подписки списывает счёт и признаёт выручку")
    void postingSubscriptionDebitsAccountAndRecognizesRevenue() {
        Customer customer = customer();
        Tariff tariff = tariff("Про", "2000.00", 30, true);
        Payment topUp = payments.save(payment(customer, "5000.00"));
        posting.post(topUp);

        Subscription subscription = new Subscription();
        subscription.setCustomer(Ref.of(Customer.class, customer.getId()));
        subscription.getLines().add(line(tariff, 1));
        Subscription saved = subscriptions.save(subscription);
        posting.post(saved);

        assertThat(balance(customer)).isEqualByComparingTo("3000.00");
        List<TariffRevenue> rows = revenue.getTurnover(
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1),
                f -> f.where(TariffRevenue::getCustomer, Ref.of(Customer.class, customer.getId()))
                        .where(TariffRevenue::getTariff, Ref.of(Tariff.class, tariff.getId())));
        assertThat(rows).isNotEmpty();
        BigDecimal amount = rows.stream().map(TariffRevenue::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal periods = rows.stream().map(TariffRevenue::getPeriods).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(amount).isEqualByComparingTo("2000.00");
        assertThat(periods).isEqualByComparingTo("1");
    }

    @Test
    @DisplayName("BR-CANCEL: отменённая подписка не создаёт движений")
    void cancelledSubscriptionPostsNoMovements() {
        Customer customer = customer();
        Tariff tariff = yearlyTariff();
        Payment payment = payment(customer, "20000.00");
        payments.save(payment);
        posting.post(payment);

        Subscription subscription = new Subscription();
        subscription.setCustomer(Ref.of(Customer.class, customer.getId()));
        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription.setCancelReason("Отказ клиента");
        subscription.getLines().add(line(tariff, 1));
        Subscription saved = subscriptions.save(subscription);
        posting.post(saved);

        assertThat(balance(customer)).isEqualByComparingTo("20000.00");
        assertThat(revenue.getRecordsByDocument(saved.getId())).isEmpty();
        assertThat(accounts.getRecordsByDocument(saved.getId())).isEmpty();
    }

    @Test
    @DisplayName("BR-FUNDS: провести подписку при нехватке денег нельзя")
    void postingFailsWhenAccountIsShort() {
        Customer customer = customer();
        Tariff tariff = tariff("Дорогой", "9999.00", 30, true);
        Subscription subscription = new Subscription();
        subscription.setCustomer(Ref.of(Customer.class, customer.getId()));
        subscription.getLines().add(line(tariff, 1));
        Subscription saved = subscriptions.save(subscription);

        assertThatThrownBy(() -> posting.post(saved))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("negative balance");
        Subscription reloaded = subscriptions.findActiveById(saved.getId()).orElseThrow();
        assertThat(reloaded.isPosted()).isFalse();
        assertThat(accounts.getRecordsByDocument(saved.getId())).isEmpty();
    }

    @Test
    @DisplayName("BR-JOB: регламент переводит подписки в Активна и Истекла, отменённые не трогает")
    void scheduledJobUpdatesStatuses() {
        Customer customer = customer();
        Tariff tariff = yearlyTariff();
        Payment topUp = payments.save(payment(customer, "20000.00"));
        posting.post(topUp);

        Subscription toActivate = new Subscription();
        toActivate.setCustomer(Ref.of(Customer.class, customer.getId()));
        toActivate.setStartDate(LocalDateTime.now().minusHours(1));
        toActivate.setStatus(SubscriptionStatus.DRAFT);
        toActivate.getLines().add(line(tariff, 1));
        toActivate = subscriptions.save(toActivate);

        Subscription toExpire = new Subscription();
        toExpire.setCustomer(Ref.of(Customer.class, customer.getId()));
        toExpire.setStartDate(LocalDateTime.now().minusDays(400));
        toExpire.setStatus(SubscriptionStatus.ACTIVE);
        toExpire.getLines().add(line(tariff, 1));
        toExpire = subscriptions.save(toExpire);
        toExpire.setEndDate(LocalDateTime.now().minusDays(1));
        toExpire.setStatus(SubscriptionStatus.ACTIVE);
        toExpire = subscriptions.save(toExpire);

        Subscription cancelled = new Subscription();
        cancelled.setCustomer(Ref.of(Customer.class, customer.getId()));
        cancelled.setStatus(SubscriptionStatus.CANCELLED);
        cancelled.setStartDate(LocalDateTime.now().minusDays(10));
        cancelled.setEndDate(LocalDateTime.now().minusDays(1));
        cancelled.getLines().add(line(tariff, 1));
        cancelled = subscriptions.save(cancelled);

        statusJob.execute();

        assertThat(subscriptions.findActiveById(toActivate.getId()).orElseThrow().getStatus())
                .isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(subscriptions.findActiveById(toExpire.getId()).orElseThrow().getStatus())
                .isEqualTo(SubscriptionStatus.EXPIRED);
        assertThat(subscriptions.findActiveById(cancelled.getId()).orElseThrow().getStatus())
                .isEqualTo(SubscriptionStatus.CANCELLED);
    }

    private Customer customer() {
        Customer customer = new Customer();
        customer.setDescription("Клиент " + SEQ.incrementAndGet() + " " + UUID.randomUUID());
        customer.setEmail("c" + SEQ.incrementAndGet() + "@example.com");
        customer.setPhone("+7 900 000-00-00");
        customer.setStatus(CustomerStatus.ACTIVE);
        return customers.save(customer);
    }

    private Tariff yearlyTariff() {
        return tariff("Год " + SEQ.incrementAndGet(), "9900.00", 365, true);
    }

    private Tariff tariff(String name, String price, int days, boolean available) {
        Tariff tariff = new Tariff();
        tariff.setDescription(name + " " + UUID.randomUUID());
        tariff.setPricePerPeriod(new BigDecimal(price));
        tariff.setPeriodDays(days);
        tariff.setAvailableForSignup(available);
        return tariffs.save(tariff);
    }

    private static SubscriptionLine line(Tariff tariff, int periods) {
        SubscriptionLine line = new SubscriptionLine();
        line.setTariff(Ref.of(Tariff.class, tariff.getId()));
        line.setPeriods(periods);
        return line;
    }

    private static Payment payment(Customer customer, String amount) {
        Payment payment = new Payment();
        payment.setCustomer(Ref.of(Customer.class, customer.getId()));
        payment.setAmount(new BigDecimal(amount));
        payment.setMethod(PaymentMethod.CARD);
        return payment;
    }

    private BigDecimal balance(Customer customer) {
        List<CustomerAccount> rows = accounts.getBalance(
                f -> f.where(CustomerAccount::getCustomer, Ref.of(Customer.class, customer.getId())));
        return rows.stream()
                .map(CustomerAccount::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
