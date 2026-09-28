package com.backend.modules.notification.entity;

/**
 * Canal de livraison d'une notification.
 */
public enum CanalNotification {
    /** Notification in-app (WebSocket / stockée en base). */
    IN_APP,
    /** Email via SendGrid SMTP relay. */
    EMAIL,
    /** Web Push (navigateur). */
    WEB_PUSH,
    /** Webhook n8n (pour orchestration externe). */
    N8N_WEBHOOK
}
