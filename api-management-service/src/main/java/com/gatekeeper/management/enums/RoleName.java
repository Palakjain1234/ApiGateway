package com.gatekeeper.management.enums;

/**
 * Fixed set of management roles in the GateKeeper control plane.
 *
 * PLATFORM_ADMIN   → owns and operates GateKeeper itself (creates orgs, suspends orgs, etc.)
 * TENANT_ADMIN     → manages one organization's Gateway configuration
 * GATEWAY_SERVICE  → machine identity used by Smart Gateway to read active route config
 */
public enum RoleName {
    PLATFORM_ADMIN,
    TENANT_ADMIN,
    GATEWAY_SERVICE
}
