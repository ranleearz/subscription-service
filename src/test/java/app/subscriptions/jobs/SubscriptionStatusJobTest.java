package app.subscriptions.jobs;

import app.subscriptions.domain.documents.Subscription;
import app.subscriptions.domain.enums.SubscriptionStatus;
import app.subscriptions.repositories.SubscriptionRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SubscriptionStatusJobTest {

    private final SubscriptionStatusJob job = new SubscriptionStatusJob(mock(SubscriptionRepository.class));

    @Test
    @DisplayName("BR-JOB: оформленная подписка с наступившей датой начала становится активной")
    void draftBecomesActiveWhenStartDateArrives() {
        Subscription subscription = subscription(
                SubscriptionStatus.DRAFT,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(30));

        assertThat(job.applyStatus(subscription, LocalDateTime.now())).isTrue();
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    @DisplayName("BR-JOB: подписка с прошедшей датой окончания становится истёкшей")
    void becomesExpiredWhenEndDatePasses() {
        Subscription subscription = subscription(
                SubscriptionStatus.ACTIVE,
                LocalDateTime.now().minusDays(40),
                LocalDateTime.now().minusDays(1));

        assertThat(job.applyStatus(subscription, LocalDateTime.now())).isTrue();
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.EXPIRED);
    }

    @Test
    @DisplayName("BR-JOB: отменённая подписка не меняет статус")
    void cancelledIsLeftUntouched() {
        Subscription subscription = subscription(
                SubscriptionStatus.CANCELLED,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().minusHours(1));

        assertThat(job.applyStatus(subscription, LocalDateTime.now())).isFalse();
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
    }

    private static Subscription subscription(SubscriptionStatus status, LocalDateTime start, LocalDateTime end) {
        Subscription subscription = new Subscription();
        subscription.setStatus(status);
        subscription.setStartDate(start);
        subscription.setEndDate(end);
        return subscription;
    }
}
