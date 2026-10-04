package com.backend.modules.notification.entity;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class NotificationReceptionId implements Serializable {

    private Long notification;
    private Long utilisateur;
}
