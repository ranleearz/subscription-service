package app.subscriptions.domain.catalogs;

import su.onno.annotations.AccessControl;
import su.onno.annotations.Attribute;
import su.onno.annotations.Catalog;
import su.onno.model.CatalogObject;

import java.math.BigDecimal;

@Catalog(name = "Tariffs", title = "Тариф", codePrefix = "T-", context = "Billing")
@AccessControl(readRoles = {"ADMIN"}, writeRoles = {"ADMIN"})
public class Tariff extends CatalogObject {

    @Attribute(displayName = "Цена за период", precision = 15, scale = 2)
    private BigDecimal pricePerPeriod = BigDecimal.ZERO;

    @Attribute(displayName = "Длительность периода, дни")
    private Integer periodDays = 30;

    @Attribute(displayName = "Доступен для подключения")
    private Boolean availableForSignup = Boolean.TRUE;

    public BigDecimal getPricePerPeriod() {
        return pricePerPeriod;
    }

    public void setPricePerPeriod(BigDecimal pricePerPeriod) {
        this.pricePerPeriod = pricePerPeriod;
    }

    public Integer getPeriodDays() {
        return periodDays;
    }

    public void setPeriodDays(Integer periodDays) {
        this.periodDays = periodDays;
    }

    public Boolean getAvailableForSignup() {
        return availableForSignup;
    }

    public void setAvailableForSignup(Boolean availableForSignup) {
        this.availableForSignup = availableForSignup;
    }
}
