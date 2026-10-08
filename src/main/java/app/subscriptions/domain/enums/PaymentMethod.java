package app.subscriptions.domain.enums;

import su.onno.annotations.EnumLabel;
import su.onno.annotations.Enumeration;

@Enumeration(name = "PaymentMethods", title = "Способ оплаты")
public enum PaymentMethod {

    @EnumLabel(value = "Банковская карта", color = "#2563EB")
    CARD,

    @EnumLabel(value = "Банковский перевод", color = "#7C3AED")
    BANK_TRANSFER,

    @EnumLabel(value = "Наличные", color = "#059669")
    CASH
}
