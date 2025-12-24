package com.github.msvidal.transactionauthorizer.domain.service;

import com.github.msvidal.transactionauthorizer.domain.model.WebhookEvent;

public interface WebhookNotificationService {
    /**
     * Sends a webhook notification for the given event
     * @param event The webhook event to send
     */
    void notify(WebhookEvent event);
}
