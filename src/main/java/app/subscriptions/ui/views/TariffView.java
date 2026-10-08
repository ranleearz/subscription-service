package app.subscriptions.ui.views;

import app.subscriptions.domain.catalogs.Tariff;
import su.onno.ui.EntityConfigBuilder;
import su.onno.ui.EntityView;
import su.onno.ui.ListSpec;

import org.springframework.stereotype.Component;

@Component
public class TariffView implements EntityView<Tariff> {

    @Override
    public Class<Tariff> entity() {
        return Tariff.class;
    }

    @Override
    public void list(ListSpec<Tariff> list) {
        list.columns(Tariff::getDescription, Tariff::getPricePerPeriod, Tariff::getPeriodDays,
                        Tariff::getAvailableForSignup)
                .label(Tariff::getDescription, "Тариф")
                .label(Tariff::getPricePerPeriod, "Цена")
                .label(Tariff::getPeriodDays, "Дней")
                .sortBy(Tariff::getDescription, false);
        list.filter(Tariff::getAvailableForSignup).label("Доступен");
    }

    @Override
    public void fields(EntityConfigBuilder<Tariff> f) {
        f.field(Tariff::getDescription).order(0).label("Название").width("half")
            .field(Tariff::getAvailableForSignup).order(1).width("half")
                .hint("Недоступный тариф нельзя включить в новую подписку.")
            .field(Tariff::getPricePerPeriod).order(2).width("half").format("currency:RUB")
                .hint("Цена одного периода. Подставляется в строку подписки.")
            .field(Tariff::getPeriodDays).order(3).width("half")
                .hint("Длительность одного периода в днях.");
    }
}
