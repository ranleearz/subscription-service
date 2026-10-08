package app.subscriptions.ui.layouts;

import app.subscriptions.domain.catalogs.Customer;
import app.subscriptions.domain.catalogs.Tariff;
import app.subscriptions.domain.documents.Payment;
import app.subscriptions.domain.documents.Subscription;
import app.subscriptions.domain.registers.CustomerAccount;
import app.subscriptions.domain.registers.TariffRevenue;
import su.onno.ui.Layout;
import su.onno.ui.LayoutSpec;
import su.onno.ui.NavStyle;

import org.springframework.stereotype.Component;

@Component
public class MainLayout implements Layout {

    @Override
    public void configure(LayoutSpec layout) {
        layout.shell()
                .nav(NavStyle.SIDEBAR)
                .brand("Подписки")
                .light(c -> c.primary("#2563EB").primarySoft("#DBEAFE"))
                .dark(c -> c.primary("#60A5FA").primarySoft("#172554"));

        layout.section("Обзор")
                .order(0)
                .icon("layout-dashboard")
                .page("/", "Дашборд", "activity");

        layout.section("Клиенты")
                .order(1)
                .icon("users")
                .catalog(Customer.class);

        layout.section("Тарифы")
                .order(2)
                .icon("badge-percent")
                .catalog(Tariff.class);

        layout.section("Документы")
                .order(3)
                .icon("file-text")
                .document(Payment.class)
                .document(Subscription.class);

        layout.section("Отчёты")
                .order(4)
                .icon("chart-column")
                .register(CustomerAccount.class)
                .register(TariffRevenue.class);
    }
}
