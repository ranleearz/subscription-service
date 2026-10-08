package app.subscriptions.ui.views;

import app.subscriptions.domain.documents.Payment;
import su.onno.ui.EntityConfigBuilder;
import su.onno.ui.EntityView;
import su.onno.ui.ListSpec;

import org.springframework.stereotype.Component;

@Component
public class PaymentView implements EntityView<Payment> {

    @Override
    public Class<Payment> entity() {
        return Payment.class;
    }

    @Override
    public void list(ListSpec<Payment> list) {
        list.columns(Payment::getNumber, Payment::getDate, Payment::getCustomer,
                        Payment::getAmount, Payment::getMethod, Payment::isPosted)
                .label(Payment::getAmount, "Сумма")
                .sortBy(Payment::getDate, true)
                .groupable(Payment::getMethod, Payment::getCustomer)
                .aggregate(Payment::getAmount, ListSpec.Agg.SUM, "Сумма");
        list.filter(Payment::getMethod).label("Способ оплаты").multiOptions();
        list.filter(Payment::getDate).label("Дата").dateRange();
    }

    @Override
    public void fields(EntityConfigBuilder<Payment> f) {
        f.field(Payment::getCustomer).order(0).width("half")
                .hint("Чей лицевой счёт пополняется.")
            .field(Payment::getMethod).order(1).width("half")
            .field(Payment::getDate).order(2).width("half").format("dd.MM.yyyy HH:mm")
            .field(Payment::getAmount).order(3).width("half").format("currency:RUB")
                .hint("Сумма зачисляется на лицевой счёт при проведении.");
        f.action("post").primary();
    }
}
