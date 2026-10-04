package com.backend.modules.utilisateur.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "permission", uniqueConstraints = {
        @UniqueConstraint(name = "uk_permission_code", columnNames = "code")
})
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_permission")
    private Long id;

    @Column(nullable = false, length = 80)
    private String code;

    @Column(length = 100)
    private String module;

    @Column(length = 100)
    private String action;
}
