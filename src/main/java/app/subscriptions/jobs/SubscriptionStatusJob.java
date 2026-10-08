package app.subscriptions.jobs;

import app.subscriptions.domain.documents.Subscription;
import app.subscriptions.domain.enums.SubscriptionStatus;
import app.subscriptions.repositories.SubscriptionRepository;
import su.onno.annotations.ScheduledJob;
import su.onno.jobs.BackgroundTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@ScheduledJob(name = "SubscriptionStatusUpdate", cron = "0 * * * * *")
public class SubscriptionStatusJob implements BackgroundTask {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionStatusJob.class);

    private final SubscriptionRepository subscriptions;

    public SubscriptionStatusJob(SubscriptionRepository subscriptions) {
        this.subscriptions = subscriptions;
    }

    @Override
    public void execute() {
        LocalDateTime now = LocalDateTime.now();
        int changed = 0;
        for (Subscription subscription : subscriptions.findAllActive()) {
            if (applyStatus(subscription, now)) {
                subscriptions.save(subscription);
                changed++;
            }
        }
        if (changed > 0) {
            log.info("Updated status for {} subscription(s)", changed);
        }
    }

    /**
     * Переводит подписку в «Активна» / «Истекла» по датам. Отменённые не трогает.
     *
     * @return {@code true}, если статус изменился
     */
    public boolean applyStatus(Subscription subscription, LocalDateTime now) {
        if (subscription.getStatus() == SubscriptionStatus.CANCELLED) {
            return false;
        }
        LocalDateTime start = subscription.getStartDate();
        LocalDateTime end = subscription.getEndDate();
        SubscriptionStatus next = subscription.getStatus();
        if (end != null && !end.isAfter(now)) {
            next = SubscriptionStatus.EXPIRED;
        } else if (start != null && !start.isAfter(now) && (end == null || end.isAfter(now))) {
            next = SubscriptionStatus.ACTIVE;
        }
        if (next == subscription.getStatus()) {
            return false;
        }
        subscription.setStatus(next);
        return true;
    }
}
