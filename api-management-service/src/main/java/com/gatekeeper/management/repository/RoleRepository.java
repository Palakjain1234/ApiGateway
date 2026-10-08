package com.gatekeeper.management.repository;

import com.gatekeeper.management.entity.Role;
import com.gatekeeper.management.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Provides read access to the fixed set of roles.
 * Roles are seeded by Flyway and never created at runtime.
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleName name);
}
