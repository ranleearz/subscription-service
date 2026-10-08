# Трассировка требований к тестам

Каждое бизнес-правило из постановки имеет стабильный идентификатор (`BR-*`).
Он указан в `@DisplayName` теста и в этой таблице.

| ID | Требование | Где в коде | Тест |
| --- | --- | --- | --- |
| BR-CLIENT | Клиент обязателен для платежа и подписки | `Payment.rules`, `Subscription.rules` | `BillingIntegrationTest` |
| BR-LINES | В подписке хотя бы одна строка | `Subscription.rules` (`lines-required`) | `BillingIntegrationTest` |
| BR-PERIODS | Число периодов больше нуля | `Subscription.rules` (`periods-positive`) | `BillingIntegrationTest` |
| BR-TARIFF | Тариф доступен для подключения | `Subscription.rules` (`tariff-available`) | `BillingIntegrationTest` |
| BR-FILL | Дата документа и дата начала при создании | `Subscription.onFilling` | `BillingIntegrationTest` |
| BR-PRICE | Цена строки из тарифа; суммы при сохранении | `Subscription.beforeWrite` | `BillingIntegrationTest` |
| BR-ENDDATE | Окончание = начало + max срок среди строк | `Subscription.beforeWrite` | `BillingIntegrationTest` |
| BR-PAY | Платёж пополняет лицевой счёт | `Payment.handlePosting` | `BillingIntegrationTest` |
| BR-SUB | Подписка списывает счёт и признаёт выручку | `Subscription.handlePosting` | `BillingIntegrationTest` |
| BR-CANCEL | Отменённая подписка не создаёт движений | `Subscription.handlePosting` | `BillingIntegrationTest` |
| BR-FUNDS | Нельзя провести подписку при нехватке денег | регистр `CustomerAccounts` (`allowNegative = false`) | `BillingIntegrationTest` |
| BR-JOB | Перевод в «Активна» / «Истекла»; отмена не трогается | `SubscriptionStatusJob` | `SubscriptionStatusJobTest`, `BillingIntegrationTest` |

Запуск:

```bash
./gradlew test
```
