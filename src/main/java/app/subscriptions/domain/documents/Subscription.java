package app.subscriptions.domain.documents;

import app.subscriptions.domain.catalogs.Customer;
import app.subscriptions.domain.catalogs.Tariff;
import app.subscriptions.domain.enums.SubscriptionStatus;
import app.subscriptions.domain.registers.CustomerAccount;
import app.subscriptions.domain.registers.TariffRevenue;
import app.subscriptions.support.DomainContext;
import su.onno.annotations.AccessControl;
import su.onno.annotations.Attribute;
import su.onno.annotations.Document;
import su.onno.annotations.TabularSection;
import su.onno.lifecycle.BeforeWriteHandler;
import su.onno.lifecycle.OnFillingHandler;
import su.onno.lifecycle.Postable;
import su.onno.model.DocumentObject;
import su.onno.posting.PostingContext;
import su.onno.rules.BusinessRule;
import su.onno.rules.Validated;
import su.onno.types.Ref;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(name = "Subscriptions", title = "Подписка", numberPrefix = "SUB-", context = "Billing")
@AccessControl(readRoles = {"ADMIN"}, writeRoles = {"ADMIN"})
public class Subscription extends DocumentObject
        implements OnFillingHandler, BeforeWriteHandler, Validated, Postable {

    @Attribute(displayName = "Клиент", required = true)
    private Ref<Customer> customer;

    @Attribute(displayName = "Статус")
    private SubscriptionStatus status = SubscriptionStatus.DRAFT;

    @Attribute(displayName = "Дата начала")
    private LocalDateTime startDate;

    @Attribute(displayName = "Дата окончания")
    private LocalDateTime endDate;

    @Attribute(displayName = "Итог", precision = 15, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Attribute(displayName = "Причина отмены", length = 1000)
    private String cancelReason;

    /** Производный признак для KPI дашборда: {@code active = true}. */
    @Attribute(displayName = "Активна")
    private Boolean active;

    @TabularSection(name = "lines")
    private List<SubscriptionLine> lines = new ArrayList<>();

    public Ref<Customer> getCustomer() {
        return customer;
    }

    public void setCustomer(Ref<Customer> customer) {
        this.customer = customer;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public void setStatus(SubscriptionStatus status) {
        this.status = status;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public List<SubscriptionLine> getLines() {
        return lines;
    }

    public void setLines(List<SubscriptionLine> lines) {
        this.lines = lines;
    }

    @Override
    public void onFilling() {
        if (getDate() == null) {
            setDate(LocalDateTime.now());
        }
        if (startDate == null) {
            startDate = LocalDateTime.now();
        }
        if (status == null) {
            status = SubscriptionStatus.DRAFT;
        }
    }

    @Override
    public void beforeWrite() {
        BigDecimal sum = BigDecimal.ZERO;
        int maxDays = 0;
        if (lines != null) {
            for (SubscriptionLine line : lines) {
                applyTariffPrice(line);
                int periods = line.getPeriods() == null ? 0 : line.getPeriods();
                BigDecimal price = nvl(line.getPrice());
                BigDecimal amount = price.multiply(BigDecimal.valueOf(periods));
                line.setAmount(amount);
                sum = sum.add(amount);
                maxDays = Math.max(maxDays, durationDays(line, periods));
            }
        }
        this.total = sum;
        if (startDate != null) {
            this.endDate = startDate.plusDays(maxDays);
        }
        this.active = status == SubscriptionStatus.ACTIVE;
    }

    @Override
    public List<BusinessRule> rules() {
        boolean cancelled = status == SubscriptionStatus.CANCELLED;
        List<BusinessRule> result = new ArrayList<>();
        result.add(BusinessRule.onField("customer", "Укажите клиента", () -> customer != null));
        result.add(new BusinessRule("lines-required", "Добавьте хотя бы одну строку тарифа",
                () -> cancelled || (lines != null && !lines.isEmpty())));
        result.add(new BusinessRule("periods-positive", "Число периодов должно быть больше нуля",
                () -> cancelled || allPeriodsPositive()));
        result.add(new BusinessRule("tariff-available", "Тариф должен быть доступен для подключения",
                () -> cancelled || allTariffsAvailable()));
        return result;
    }

    @Override
    public void handlePosting(PostingContext context) {
        if (status == SubscriptionStatus.CANCELLED) {
            return;
        }
        if (customer != null && total != null && total.signum() > 0) {
            context.movements(CustomerAccount.class).addExpense(r -> {
                r.setCustomer(customer);
                r.setAmount(total);
            });
        }
        var revenue = context.movements(TariffRevenue.class);
        if (lines == null) {
            return;
        }
        for (SubscriptionLine line : lines) {
            if (line.getTariff() == null) {
                continue;
            }
            revenue.addReceipt(r -> {
                r.setTariff(line.getTariff());
                r.setCustomer(customer);
                r.setAmount(nvl(line.getAmount()));
                int periods = line.getPeriods() == null ? 0 : line.getPeriods();
                r.setPeriods(BigDecimal.valueOf(periods));
            });
        }
    }

    private void applyTariffPrice(SubscriptionLine line) {
        DomainContext.tariff(line.getTariff()).ifPresent(tariff -> {
            if (tariff.getPricePerPeriod() != null) {
                line.setPrice(tariff.getPricePerPeriod());
            }
        });
    }

    private int durationDays(SubscriptionLine line, int periods) {
        return DomainContext.tariff(line.getTariff())
                .map(Tariff::getPeriodDays)
                .filter(days -> days != null && days > 0)
                .map(days -> days * Math.max(periods, 0))
                .orElse(0);
    }

    private boolean allPeriodsPositive() {
        if (lines == null) {
            return true;
        }
        return lines.stream().allMatch(line -> line.getPeriods() != null && line.getPeriods() > 0);
    }

    private boolean allTariffsAvailable() {
        if (lines == null || lines.isEmpty()) {
            return true;
        }
        return lines.stream().allMatch(line -> {
            if (line.getTariff() == null) {
                return false;
            }
            return DomainContext.tariff(line.getTariff())
                    .map(tariff -> Boolean.TRUE.equals(tariff.getAvailableForSignup()))
                    .orElse(false);
        });
    }

    private static BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
