package app.subscriptions.domain.registers;

import app.subscriptions.domain.catalogs.Customer;
import app.subscriptions.domain.catalogs.Tariff;
import su.onno.annotations.AccessControl;
import su.onno.annotations.AccumulationRegister;
import su.onno.annotations.Dimension;
import su.onno.annotations.Resource;
import su.onno.model.AccumulationRecord;
import su.onno.model.AccumulationType;
import su.onno.types.Ref;

import java.math.BigDecimal;

@AccumulationRegister(
        name = "TariffRevenue",
        title = "Выручка по тарифам",
        type = AccumulationType.TURNOVER,
        context = "Billing")
@AccessControl(readRoles = {"ADMIN"})
public class TariffRevenue extends AccumulationRecord {

    @Dimension(displayName = "Тариф")
    private Ref<Tariff> tariff;

    @Dimension(displayName = "Клиент")
    private Ref<Customer> customer;

    @Resource(displayName = "Сумма", precision = 15, scale = 2)
    private BigDecimal amount;

    @Resource(displayName = "Периодов", precision = 12, scale = 0)
    private BigDecimal periods;

    public Ref<Tariff> getTariff() {
        return tariff;
    }

    public void setTariff(Ref<Tariff> tariff) {
        this.tariff = tariff;
    }

    public Ref<Customer> getCustomer() {
        return customer;
    }

    public void setCustomer(Ref<Customer> customer) {
        this.customer = customer;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getPeriods() {
        return periods;
    }

    public void setPeriods(BigDecimal periods) {
        this.periods = periods;
    }
}
