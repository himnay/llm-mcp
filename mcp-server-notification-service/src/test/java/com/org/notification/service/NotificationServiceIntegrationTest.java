package com.org.notification.service;

import com.org.notification.TestcontainersConfiguration;
import com.org.notification.model.Notification;
import com.org.notification.model.NotificationChannel;
import com.org.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class NotificationServiceIntegrationTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @BeforeEach
    void cleanup() {
        notificationRepository.deleteAll();
    }

    @DisplayName("Persists an EMAIL notification with recipient, message, and timestamp")
    @Test
    void sendNotification_persistsEmailNotification() {
        Notification saved = notificationService.sendNotification(
                NotificationChannel.EMAIL, "alice@example.com", "Your report is ready");

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getChannel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(saved.getRecipient()).isEqualTo("alice@example.com");
        assertThat(saved.getMessage()).isEqualTo("Your report is ready");
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @DisplayName("Persists a SLACK notification with the correct channel and recipient")
    @Test
    void sendNotification_persistsSlackNotification() {
        Notification saved = notificationService.sendNotification(
                NotificationChannel.SLACK, "#ops-alerts", "Deploy finished");

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getChannel()).isEqualTo(NotificationChannel.SLACK);
        assertThat(saved.getRecipient()).isEqualTo("#ops-alerts");
    }

    @DisplayName("Persists an INTERNAL notification with the correct channel")
    @Test
    void sendNotification_persistsInternalNotification() {
        Notification saved = notificationService.sendNotification(
                NotificationChannel.INTERNAL, "bob", "You have a new ticket assigned");

        assertThat(saved.getChannel()).isEqualTo(NotificationChannel.INTERNAL);
    }

    @DisplayName("Returns all previously sent notifications across all channels")
    @Test
    void getNotifications_returnsAllSaved() {
        notificationService.sendNotification(NotificationChannel.EMAIL, "a@a.com", "msg1");
        notificationService.sendNotification(NotificationChannel.SLACK, "#general", "msg2");
        notificationService.sendNotification(NotificationChannel.INTERNAL, "carol", "msg3");

        List<Notification> all = notificationService.getNotifications();

        assertThat(all).hasSize(3);
        assertThat(all).extracting(Notification::getChannel)
                .containsExactlyInAnyOrder(
                        NotificationChannel.EMAIL,
                        NotificationChannel.SLACK,
                        NotificationChannel.INTERNAL);
    }

    @DisplayName("Returns an empty list when no notifications have been sent")
    @Test
    void getNotifications_returnsEmptyWhenNoneSent() {
        assertThat(notificationService.getNotifications()).isEmpty();
    }
}
