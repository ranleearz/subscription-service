package app.subscriptions.domain.catalogs;

import app.subscriptions.domain.enums.CustomerStatus;
import su.onno.annotations.AccessControl;
import su.onno.annotations.Attribute;
import su.onno.annotations.Catalog;
import su.onno.lifecycle.OnFillingHandler;
import su.onno.model.CatalogObject;

import java.time.LocalDateTime;

@Catalog(name = "Customers", title = "Клиент", codePrefix = "C-", context = "Billing")
@AccessControl(readRoles = {"ADMIN"}, writeRoles = {"ADMIN"})
public class Customer extends CatalogObject implements OnFillingHandler {

    @Attribute(displayName = "Статус")
    private CustomerStatus status = CustomerStatus.ACTIVE;

    @Attribute(displayName = "E-mail", length = 200, email = true)
    private String email;

    @Attribute(displayName = "Телефон", length = 50)
    private String phone;

    @Attribute(displayName = "Дата регистрации")
    private LocalDateTime registrationDate;

    public CustomerStatus getStatus() {
        return status;
    }

    public void setStatus(CustomerStatus status) {
        this.status = status;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public LocalDateTime getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(LocalDateTime registrationDate) {
        this.registrationDate = registrationDate;
    }

    @Override
    public void onFilling() {
        if (registrationDate == null) {
            registrationDate = LocalDateTime.now();
        }
        if (status == null) {
            status = CustomerStatus.ACTIVE;
        }
    }
}
