package app.subscriptions.ui.pages;

import app.subscriptions.domain.documents.Payment;
import app.subscriptions.domain.documents.Subscription;
import su.onno.ui.Page;
import su.onno.ui.PageBuilder;

import org.springframework.stereotype.Component;

@Component
public class DashboardPage implements Page {

    @Override
    public String route() {
        return "/";
    }

    @Override
    public void compose(PageBuilder b) {
        b.title("Дашборд");
        b.subtitle("Остатки лицевых счетов, выручка и последние документы");

        b.widget("Период").type("timeRange").width("full").order(-10)
                .config("presets", "7d,30d,90d,1y,all")
                .config("default", "30d");

        b.widget("Активные подписки").type("stat").width("1/4").order(0)
                .document(Subscription.class)
                .dateField(Subscription::getDate)
                .config("metric", "count")
                .config("filter", "active = true")
                .config("trend", "false")
                .hint("Подписки в статусе «Активна».");

        b.widget("Платежи").type("stat").width("1/4").order(1)
                .document(Payment.class)
                .dateField(Payment::getDate)
                .config("metric", "sum").metricField(Payment::getAmount)
                .config("currency", "RUB")
                .config("filter", "_posted = true")
                .config("trend", "false")
                .config("comparison", "true")
                .config("comparisonLabel", "к прошлому периоду");

        b.widget("Выручка по подпискам").type("stat").width("1/4").order(2)
                .document(Subscription.class)
                .dateField(Subscription::getDate)
                .config("metric", "sum").metricField(Subscription::getTotal)
                .config("currency", "RUB")
                .config("filter", "_posted = true")
                .config("trend", "false")
                .config("comparison", "true")
                .config("comparisonLabel", "к прошлому периоду");

        b.widget("Всего подписок").type("stat").width("1/4").order(3)
                .document(Subscription.class)
                .dateField(Subscription::getDate)
                .config("metric", "count")
                .config("trend", "false")
                .config("comparison", "true");

        b.widget("Подписки по статусу").type("chart").width("1/2").order(10)
                .document(Subscription.class)
                .config("kind", "pie")
                .config("groupBy", "statusDisplay")
                .config("metric", "count");

        b.widget("Выручка по дням").type("chart").width("1/2").order(11)
                .document(Subscription.class)
                .config("kind", "area")
                .groupBy(Subscription::getDate).config("groupByDate", "day")
                .config("metric", "sum").metricField(Subscription::getTotal)
                .config("filter", "_posted = true");

        b.widget("Поступления на счета").type("chart").width("full").order(12)
                .document(Payment.class)
                .config("kind", "bar")
                .groupBy(Payment::getDate).config("groupByDate", "week")
                .config("metric", "sum").metricField(Payment::getAmount)
                .config("filter", "_posted = true")
                .config("currency", "RUB");

        b.widget("Последние подписки").type("list").width("1/2").order(20)
                .document(Subscription.class).maxItems(8)
                .config("titleTemplate", "{number} · {customerDisplay}")
                .config("secondaryField", "statusDisplay");

        b.widget("Последние платежи").type("list").width("1/2").order(21)
                .document(Payment.class).maxItems(8)
                .config("titleTemplate", "{number} · {customerDisplay}")
                .config("secondaryField", "amount");
    }
}
