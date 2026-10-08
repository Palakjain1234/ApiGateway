-- =============================================================================
-- GateKeeper :: API Management Service — Initial Schema
-- Flyway migration V1
-- =============================================================================

-- ─────────────────────────────────────────────────────────────────────────────
-- 1. roles
--    Stores the fixed set of management roles.
--    PLATFORM_ADMIN | TENANT_ADMIN | GATEWAY_SERVICE
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE roles (
    id         BIGINT        NOT NULL AUTO_INCREMENT,
    name       VARCHAR(50)   NOT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uq_roles_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO roles (name) VALUES
    ('PLATFORM_ADMIN'),
    ('TENANT_ADMIN'),
    ('GATEWAY_SERVICE');


-- ─────────────────────────────────────────────────────────────────────────────
-- 2. organizations
--    A company / tenant that uses GateKeeper.
--    slug is used as the first path segment in routes (/foodfast/orders/**)
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE organizations (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100)  NOT NULL,
    slug        VARCHAR(100)  NOT NULL,
    status      VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
                                       -- PENDING | ACTIVE | SUSPENDED | DELETED
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at  DATETIME          NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uq_organizations_slug (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ─────────────────────────────────────────────────────────────────────────────
-- 3. management_users
--    Human or machine identities that can operate the GateKeeper control plane.
--
--    organization_id is nullable:
--      PLATFORM_ADMIN  → NULL  (belongs to GateKeeper itself)
--      GATEWAY_SERVICE → NULL
--      TENANT_ADMIN    → references organizations.id
--
--    Uniqueness rule: at most one TENANT_ADMIN per organization.
--    MySQL allows multiple NULL values in a unique index, so multiple
--    platform-level accounts can coexist.
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE management_users (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    organization_id BIGINT            NULL,
    role_id         BIGINT        NOT NULL,
    username        VARCHAR(100)  NOT NULL,
    password_hash   VARCHAR(255)  NOT NULL,
    enabled         TINYINT(1)    NOT NULL DEFAULT 1,
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uq_management_users_username (username),
    -- One tenant admin per organization (NULLs are exempt)
    UNIQUE KEY uq_management_users_org (organization_id),

    CONSTRAINT fk_mgmt_users_org
        FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_mgmt_users_role
        FOREIGN KEY (role_id) REFERENCES roles (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Seed the platform admin (password: admin123 — bcrypt hash)
INSERT INTO management_users (organization_id, role_id, username, password_hash, enabled)
VALUES (
    NULL,
    (SELECT id FROM roles WHERE name = 'PLATFORM_ADMIN'),
    'platform-admin',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    1
);

-- Seed the gateway service account (password: gateway123 — bcrypt hash)
INSERT INTO management_users (organization_id, role_id, username, password_hash, enabled)
VALUES (
    NULL,
    (SELECT id FROM roles WHERE name = 'GATEWAY_SERVICE'),
    'gateway-service',
    '$2a$10$8K1p/a0dN7LnQtC6yF5JUe9rQ5OmL2XvZW8j4mS1K0pQ3yN6mR7yS',
    1
);


-- ─────────────────────────────────────────────────────────────────────────────
-- 4. api_routes
--    A route belongs to an organization and describes how a public path
--    maps to a backend service along with its Gateway policies.
--
--    Identity is (organization_id, route_id) — route_id only needs to be
--    unique within an organization, not globally.
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE api_routes (
    id                      BIGINT        NOT NULL AUTO_INCREMENT,
    organization_id         BIGINT        NOT NULL,
    route_id                VARCHAR(100)  NOT NULL,   -- human-readable e.g. "orders"
    name                    VARCHAR(200)  NOT NULL,
    path_pattern            VARCHAR(500)  NOT NULL,   -- e.g. /foodfast/orders/**
    target_base_url         VARCHAR(500)  NOT NULL,   -- e.g. http://order-service:8093
    target_path_prefix      VARCHAR(500)      NULL,   -- e.g. /api
    requests_per_minute     INT               NULL,   -- NULL = unlimited
    response_timeout_ms     INT           NOT NULL DEFAULT 30000,
    idempotency_enabled     TINYINT(1)    NOT NULL DEFAULT 0,
    active                  TINYINT(1)    NOT NULL DEFAULT 1,
    configuration_version   INT           NOT NULL DEFAULT 1,
    created_at              DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    -- Route identity is scoped to the organization
    UNIQUE KEY uq_api_routes_org_route (organization_id, route_id),

    CONSTRAINT fk_api_routes_org
        FOREIGN KEY (organization_id) REFERENCES organizations (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ─────────────────────────────────────────────────────────────────────────────
-- 5. route_allowed_methods
--    One-to-many: each route can allow multiple HTTP methods.
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE route_allowed_methods (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    route_id    BIGINT      NOT NULL,
    http_method VARCHAR(10) NOT NULL,   -- GET | POST | PUT | DELETE | PATCH

    PRIMARY KEY (id),
    UNIQUE KEY uq_route_method (route_id, http_method),

    CONSTRAINT fk_route_methods_route
        FOREIGN KEY (route_id) REFERENCES api_routes (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ─────────────────────────────────────────────────────────────────────────────
-- 6. route_header_rules
--    One-to-many: each route can have multiple header manipulation rules.
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE route_header_rules (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    route_id     BIGINT       NOT NULL,
    action       VARCHAR(10)  NOT NULL,   -- ADD | REMOVE
    header_name  VARCHAR(200) NOT NULL,
    header_value VARCHAR(500)     NULL,   -- NULL when action = REMOVE

    PRIMARY KEY (id),

    CONSTRAINT fk_route_headers_route
        FOREIGN KEY (route_id) REFERENCES api_routes (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ─────────────────────────────────────────────────────────────────────────────
-- 7. route_configuration_versions
--    Immutable history of how a route looked at each change.
--    Deliberately NOT foreign-keyed to api_routes so history survives
--    route deletion (live state and historical state have different lifecycles).
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE route_configuration_versions (
    id                      BIGINT          NOT NULL AUTO_INCREMENT,
    organization_id         BIGINT          NOT NULL,
    route_id                VARCHAR(100)    NOT NULL,
    version_number          INT             NOT NULL,
    change_type             VARCHAR(20)     NOT NULL,  -- CREATED | UPDATED | ACTIVATED | DEACTIVATED | DELETED
    configuration_snapshot  JSON            NOT NULL,  -- full route + methods + headers at this version
    created_by              VARCHAR(100)    NOT NULL,
    created_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    KEY idx_route_versions_org_route (organization_id, route_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ─────────────────────────────────────────────────────────────────────────────
-- 8. control_plane_audit_records
--    Who did what, when, and what happened.
--    Separate from versioning — answers operator/action history.
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE control_plane_audit_records (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    organization_id BIGINT              NULL,   -- NULL for platform-level actions
    actor           VARCHAR(100)    NOT NULL,   -- username of the caller
    action          VARCHAR(100)    NOT NULL,   -- e.g. CREATE_ROUTE, UPDATE_ROUTE
    resource_type   VARCHAR(100)        NULL,   -- e.g. ROUTE, ORGANIZATION
    resource_id     VARCHAR(200)        NULL,   -- e.g. orders, 42
    correlation_id  VARCHAR(100)        NULL,   -- request trace ID
    duration_ms     BIGINT              NULL,
    result          VARCHAR(20)     NOT NULL,   -- SUCCESS | FAILURE
    details         JSON                NULL,   -- extra context
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    KEY idx_audit_actor (actor),
    KEY idx_audit_org (organization_id),
    KEY idx_audit_action (action)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
