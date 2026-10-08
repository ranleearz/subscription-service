package app.subscriptions.seed;

import app.subscriptions.domain.catalogs.Customer;
import app.subscriptions.domain.catalogs.Tariff;
import app.subscriptions.domain.documents.Payment;
import app.subscriptions.domain.documents.Subscription;
import app.subscriptions.domain.documents.SubscriptionLine;
import app.subscriptions.domain.enums.CustomerStatus;
import app.subscriptions.domain.enums.PaymentMethod;
import app.subscriptions.repositories.CustomerRepository;
import app.subscriptions.repositories.PaymentRepository;
import app.subscriptions.repositories.SubscriptionRepository;
import app.subscriptions.repositories.TariffRepository;
import su.onno.posting.PostingService;
import su.onno.types.Ref;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@ConditionalOnProperty(name = "app.demo-seed", havingValue = "true", matchIfMissing = true)
public class DemoSeeder implements ApplicationRunner {

    private final CustomerRepository customers;
    private final TariffRepository tariffs;
    private final PaymentRepository payments;
    private final SubscriptionRepository subscriptions;
    private final PostingService posting;

    public DemoSeeder(CustomerRepository customers, TariffRepository tariffs,
                      PaymentRepository payments, SubscriptionRepository subscriptions,
                      PostingService posting) {
        this.customers = customers;
        this.tariffs = tariffs;
        this.payments = payments;
        this.subscriptions = subscriptions;
        this.posting = posting;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!customers.findAllActive().isEmpty()) {
            return;
        }

        Customer anna = customer("Анна Смирнова", "anna@example.com", "+7 900 111-22-33");
        Customer igor = customer("Игорь Волков", "igor@example.com", "+7 900 444-55-66");

        Tariff yearly = tariff("Годовой доступ", "9900.00", 365, true);
        Tariff monthly = tariff("Месячный доступ", "990.00", 30, true);
        tariff("Архивный тариф", "100.00", 7, false);

        Payment payment = new Payment();
        payment.setCustomer(Ref.of(Customer.class, anna.getId()));
        payment.setAmount(new BigDecimal("15000.00"));
        payment.setMethod(PaymentMethod.CARD);
        payment.setDate(LocalDateTime.now().minusDays(2));
        payments.save(payment);
        posting.post(payment);

        Subscription subscription = new Subscription();
        subscription.setCustomer(Ref.of(Customer.class, anna.getId()));
        subscription.setStartDate(LocalDateTime.now().minusDays(1));
        subscription.getLines().add(line(yearly, 1));
        subscription.getLines().add(line(monthly, 1));
        subscriptions.save(subscription);
        posting.post(subscription);

        Payment igorPayment = new Payment();
        igorPayment.setCustomer(Ref.of(Customer.class, igor.getId()));
        igorPayment.setAmount(new BigDecimal("2000.00"));
        igorPayment.setMethod(PaymentMethod.BANK_TRANSFER);
        payments.save(igorPayment);
        posting.post(igorPayment);
    }

    private Customer customer(String name, String email, String phone) {
        Customer customer = new Customer();
        customer.setDescription(name);
        customer.setEmail(email);
        customer.setPhone(phone);
        customer.setStatus(CustomerStatus.ACTIVE);
        customer.setRegistrationDate(LocalDateTime.now().minusDays(10));
        return customers.save(customer);
    }

    private Tariff tariff(String name, String price, int days, boolean available) {
        Tariff tariff = new Tariff();
        tariff.setDescription(name);
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
}
