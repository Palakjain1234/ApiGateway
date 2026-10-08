package com.gatekeeper.management.enums;

/**
 * Lifecycle states of an Organization.
 *
 * PENDING    → provisioned but not yet ready to use the system
 * ACTIVE     → can use GateKeeper management and its Gateway configuration
 * SUSPENDED  → temporarily disabled; data is retained and can be reactivated
 * DELETED    → soft-deleted; data is retained for historical purposes
 */
public enum OrganizationStatus {
    PENDING,
    ACTIVE,
    SUSPENDED,
    DELETED
}
