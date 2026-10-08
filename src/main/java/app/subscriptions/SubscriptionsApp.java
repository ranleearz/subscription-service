package app.subscriptions;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Учёт подписок на тарифные планы. Бизнес описан типизированной Java-метамоделью onno-framework:
 * схема БД, REST API и веб-интерфейс генерируются из каталогов, документов и регистров.
 */
@SpringBootApplication
public class SubscriptionsApp {

    public static void main(String[] args) {
        SpringApplication.run(SubscriptionsApp.class, args);
    }
}
