package com.gatekeeper.management.entity;

import com.gatekeeper.management.enums.RoleName;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Fixed set of roles that control what a management user can do.
 * Seeded by Flyway — never created at runtime.
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "name", nullable = false, unique = true, length = 50)
    private RoleName name;

    public Role(RoleName name) {
        this.name = name;
    }
}
