package app.subscriptions.support;

import app.subscriptions.domain.catalogs.Tariff;
import app.subscriptions.repositories.TariffRepository;
import su.onno.types.Ref;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Bridge for domain hooks that have no Spring DI ({@code beforeWrite}, {@code rules}).
 */
@Component
public class DomainContext implements ApplicationContextAware {

    private static ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        context = applicationContext;
    }

    public static Optional<Tariff> tariff(Ref<Tariff> ref) {
        if (ref == null || context == null) {
            return Optional.empty();
        }
        return context.getBean(TariffRepository.class).findActiveById(ref.id());
    }
}
