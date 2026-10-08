package app.subscriptions.ui.views;

import app.subscriptions.domain.registers.TariffRevenue;
import su.onno.ui.EntityConfigBuilder;
import su.onno.ui.EntityView;

import org.springframework.stereotype.Component;

@Component
public class TariffRevenueView implements EntityView<TariffRevenue> {

    @Override
    public Class<TariffRevenue> entity() {
        return TariffRevenue.class;
    }

    @Override
    public void fields(EntityConfigBuilder<TariffRevenue> f) {
        f.field(TariffRevenue::getAmount).format("currency:RUB")
            .field(TariffRevenue::getPeriod).format("dd.MM.yyyy");
    }
}
