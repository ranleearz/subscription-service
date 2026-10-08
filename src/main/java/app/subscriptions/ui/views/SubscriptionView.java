package app.subscriptions.ui.views;

import app.subscriptions.domain.documents.Subscription;
import app.subscriptions.domain.documents.SubscriptionLine;
import app.subscriptions.domain.enums.SubscriptionStatus;
import app.subscriptions.repositories.SubscriptionRepository;
import su.onno.posting.PostingService;
import su.onno.ui.ActionResult;
import su.onno.ui.ActionScope;
import su.onno.ui.ActionSpec;
import su.onno.ui.ActionToast;
import su.onno.ui.EntityConfigBuilder;
import su.onno.ui.EntityView;
import su.onno.ui.InputType;
import su.onno.ui.ListSpec;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SubscriptionView implements EntityView<Subscription> {

    private final SubscriptionRepository subscriptions;
    private final PostingService posting;

    public SubscriptionView(SubscriptionRepository subscriptions, PostingService posting) {
        this.subscriptions = subscriptions;
        this.posting = posting;
    }

    @Override
    public Class<Subscription> entity() {
        return Subscription.class;
    }

    @Override
    public void list(ListSpec<Subscription> list) {
        list.columns(Subscription::getNumber, Subscription::getDate, Subscription::getCustomer,
                        Subscription::getStatus, Subscription::getStartDate, Subscription::getEndDate,
                        Subscription::getTotal, Subscription::isPosted)
                .label(Subscription::getTotal, "Итог")
                .sortBy(Subscription::getDate, true)
                .groupable(Subscription::getStatus, Subscription::getCustomer)
                .aggregate(Subscription::getTotal, ListSpec.Agg.SUM, "Итог");
        list.filter(Subscription::getStatus).label("Статус").multiOptions();
        list.filter(Subscription::getDate).label("Дата").dateRange();
        list.rowStyle(row -> {
            SubscriptionStatus status = row.enumValue(Subscription::getStatus, SubscriptionStatus.class);
            if (status == SubscriptionStatus.CANCELLED) {
                return ListSpec.RowStyle.MUTED;
            }
            if (status == SubscriptionStatus.EXPIRED) {
                return ListSpec.RowStyle.WARNING;
            }
            return null;
        });
    }

    @Override
    public void fields(EntityConfigBuilder<Subscription> f) {
        f.field(Subscription::getCustomer).order(0).width("half")
                .hint("Клиент, с лицевого счёта которого списывается стоимость.")
            .field(Subscription::getStatus).order(1).width("half")
                .hint("«Активна» и «Истекла» выставляет регламентное задание по датам.")
            .field(Subscription::getStartDate).order(2).width("half").format("dd.MM.yyyy")
                .hint("По умолчанию — дата создания.")
            .field(Subscription::getEndDate).order(3).width("half").format("dd.MM.yyyy")
                .hint("Начало плюс максимальный срок среди строк.")
            .field(Subscription::getTotal).order(4).width("half").format("currency:RUB")
                .hint("Считается из строк при каждом сохранении.")
            .field(Subscription::getCancelReason).order(5).width("full").widget("textarea")
            .field(Subscription::getActive).order(6).hideInForm().hideInList().hideInDetail();

        f.rowRefField(Subscription::getLines, SubscriptionLine::getTariff).label("Тариф");
        f.rowField(Subscription::getLines, SubscriptionLine::getPeriods).label("Периодов");
        f.rowField(Subscription::getLines, SubscriptionLine::getPrice)
                .label("Цена").format("currency:RUB");
        f.rowField(Subscription::getLines, SubscriptionLine::getAmount)
                .label("Сумма").format("currency:RUB");

        f.action("post").primary();
        f.action("cancelTop").primary();
    }

    @Override
    public void actions(ActionSpec actions) {
        actions.action("cancel").scope(ActionScope.ROW).icon("ban").label("Отменить")
                .visibleWhen(row -> row.enumValue("status", SubscriptionStatus.class)
                        != SubscriptionStatus.CANCELLED)
                .form(form -> form.input("reason").label("Причина отмены")
                        .type(InputType.TEXTAREA)
                        .placeholder("Почему подписка отменяется?")
                        .required())
                .handler(ctx -> cancel(ctx.id(), ctx.input("reason")));

        actions.action("cancelTop").scope(ActionScope.DETAIL).icon("ban").label("Отменить подписку")
                .visibleWhen(row -> row.enumValue("status", SubscriptionStatus.class)
                        != SubscriptionStatus.CANCELLED)
                .form(form -> form.input("reason").label("Причина отмены")
                        .type(InputType.TEXTAREA)
                        .placeholder("Почему подписка отменяется?")
                        .required())
                .handler(ctx -> cancel(ctx.id(), ctx.input("reason")));
    }

    private ActionResult cancel(UUID id, String reason) {
        return subscriptions.findActiveById(id).map(subscription -> {
            if (subscription.isPosted()) {
                posting.unpost(subscription);
                subscription = subscriptions.findActiveById(id).orElse(subscription);
            }
            subscription.setStatus(SubscriptionStatus.CANCELLED);
            if (reason != null && !reason.isBlank()) {
                subscription.setCancelReason(reason);
            }
            subscriptions.save(subscription);
            return ActionResult.refresh(ActionToast.success("Подписка отменена"));
        }).orElseGet(() -> ActionResult.toast(ActionToast.warning("Подписка не найдена")));
    }
}
