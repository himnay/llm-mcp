package com.org.notification.delivery;

import com.org.notification.exception.DeliveryConfigurationException;
import com.org.notification.model.Notification;
import com.org.notification.model.NotificationChannel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the Strategy registry — no Spring context.
 */
class DeliveryStrategyRegistryTest {

    @Test
    @DisplayName("Resolves a registered delivery strategy for every notification channel")
    void resolvesAStrategyForEveryChannel() {
        DeliveryStrategyRegistry registry = new DeliveryStrategyRegistry(List.of(
                new InternalDeliveryStrategy(), new EmailDeliveryStrategy(), new SlackDeliveryStrategy()));

        for (NotificationChannel channel : NotificationChannel.values()) {
            assertThat(registry.strategyFor(channel).channel()).isEqualTo(channel);
        }
    }

    @Test
    @DisplayName("Throws DeliveryConfigurationException at construction when a channel has no strategy")
    void failsFastWhenAChannelHasNoStrategy() {
        assertThatThrownBy(() -> new DeliveryStrategyRegistry(List.of(new InternalDeliveryStrategy())))
                .isInstanceOf(DeliveryConfigurationException.class)
                .hasMessageContaining("EMAIL");
    }

    @Test
    @DisplayName("Slack delivery strategy delivers a notification without throwing")
    void strategiesAcceptANotification() {
        Notification n = Notification.builder()
                .id(1L).channel(NotificationChannel.SLACK).recipient("ops").message("hi").build();
        new SlackDeliveryStrategy().deliver(n); // must not throw
    }
}
