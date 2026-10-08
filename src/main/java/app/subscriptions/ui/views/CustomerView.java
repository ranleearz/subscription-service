package app.subscriptions.ui.views;

import app.subscriptions.domain.catalogs.Customer;
import su.onno.ui.EntityConfigBuilder;
import su.onno.ui.EntityView;
import su.onno.ui.ListSpec;

import org.springframework.stereotype.Component;

@Component
public class CustomerView implements EntityView<Customer> {

    @Override
    public Class<Customer> entity() {
        return Customer.class;
    }

    @Override
    public void list(ListSpec<Customer> list) {
        list.columns(Customer::getDescription, Customer::getStatus, Customer::getEmail,
                        Customer::getPhone, Customer::getRegistrationDate)
                .label(Customer::getDescription, "Клиент")
                .sortBy(Customer::getDescription, false);
        list.filter(Customer::getStatus).label("Статус").multiOptions();
        list.filter(Customer::getEmail).label("E-mail").contains();
    }

    @Override
    public void fields(EntityConfigBuilder<Customer> f) {
        f.field(Customer::getDescription).order(0).label("Имя").width("half")
                .hint("Как клиент отображается в списках и документах.")
            .field(Customer::getStatus).order(1).width("half")
                .hint("Активный клиент может оформлять подписки.")
            .field(Customer::getEmail).order(2).width("half")
            .field(Customer::getPhone).order(3).width("half")
            .field(Customer::getRegistrationDate).order(4).width("half").format("dd.MM.yyyy HH:mm")
                .hint("Заполняется автоматически при создании.");
    }
}
