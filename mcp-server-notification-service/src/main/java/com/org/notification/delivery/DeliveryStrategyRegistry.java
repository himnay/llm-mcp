package com.org.notification.delivery;

import com.org.notification.exception.DeliveryConfigurationException;
import com.org.notification.model.NotificationChannel;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Factory (GoF Factory Method, registry style): Spring injects every
 * {@link ChannelDeliveryStrategy} bean; this class indexes them by channel and
 * hands the right one to callers. Fails fast at startup if a channel has no
 * strategy, rather than at first send.
 */
@Component
public class DeliveryStrategyRegistry {

    private final Map<NotificationChannel, ChannelDeliveryStrategy> strategies =
            new EnumMap<>(NotificationChannel.class);

    public DeliveryStrategyRegistry(List<ChannelDeliveryStrategy> discovered) {
        discovered.forEach(s -> strategies.put(s.channel(), s));
        for (NotificationChannel channel : NotificationChannel.values()) {
            if (!strategies.containsKey(channel)) {
                throw new DeliveryConfigurationException(
                        "No ChannelDeliveryStrategy registered for channel " + channel);
            }
        }
    }

    public ChannelDeliveryStrategy strategyFor(NotificationChannel channel) {
        return strategies.get(channel);
    }
}
