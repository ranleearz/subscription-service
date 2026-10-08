package app.subscriptions.domain.documents;

import app.subscriptions.domain.catalogs.Customer;
import app.subscriptions.domain.enums.PaymentMethod;
import app.subscriptions.domain.registers.CustomerAccount;
import su.onno.annotations.AccessControl;
import su.onno.annotations.Attribute;
import su.onno.annotations.Document;
import su.onno.lifecycle.OnFillingHandler;
import su.onno.lifecycle.Postable;
import su.onno.model.DocumentObject;
import su.onno.posting.PostingContext;
import su.onno.rules.BusinessRule;
import su.onno.rules.Validated;
import su.onno.types.Ref;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Document(name = "Payments", title = "Платёж", numberPrefix = "PAY-", context = "Billing")
@AccessControl(readRoles = {"ADMIN"}, writeRoles = {"ADMIN"})
public class Payment extends DocumentObject implements OnFillingHandler, Validated, Postable {

    @Attribute(displayName = "Клиент", required = true)
    private Ref<Customer> customer;

    @Attribute(displayName = "Сумма", precision = 15, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Attribute(displayName = "Способ оплаты")
    private PaymentMethod method = PaymentMethod.CARD;

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

    public PaymentMethod getMethod() {
        return method;
    }

    public void setMethod(PaymentMethod method) {
        this.method = method;
    }

    @Override
    public void onFilling() {
        if (getDate() == null) {
            setDate(LocalDateTime.now());
        }
        if (method == null) {
            method = PaymentMethod.CARD;
        }
    }

    @Override
    public List<BusinessRule> rules() {
        return List.of(
                BusinessRule.onField("customer", "Укажите клиента", () -> customer != null),
                BusinessRule.onField("amount", "Сумма платежа должна быть больше нуля",
                        () -> amount != null && amount.signum() > 0));
    }

    @Override
    public void handlePosting(PostingContext context) {
        if (customer == null || amount == null) {
            return;
        }
        context.movements(CustomerAccount.class).addReceipt(r -> {
            r.setCustomer(customer);
            r.setAmount(amount);
        });
    }
}
