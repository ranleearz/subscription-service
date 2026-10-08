package app.subscriptions.ui.views;

import app.subscriptions.domain.registers.CustomerAccount;
import su.onno.ui.EntityConfigBuilder;
import su.onno.ui.EntityView;

import org.springframework.stereotype.Component;

@Component
public class CustomerAccountView implements EntityView<CustomerAccount> {

    @Override
    public Class<CustomerAccount> entity() {
        return CustomerAccount.class;
    }

    @Override
    public void fields(EntityConfigBuilder<CustomerAccount> f) {
        f.field(CustomerAccount::getAmount).format("currency:RUB")
            .field(CustomerAccount::getPeriod).format("dd.MM.yyyy");
    }
}
