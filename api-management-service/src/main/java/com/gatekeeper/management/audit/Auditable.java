package com.gatekeeper.management.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a service method for automatic audit recording.
 *
 * Place on any service method that mutates state (creates, updates, deletes).
 * The AuditAspect intercepts the call and saves a ControlPlaneAuditRecord
 * capturing who did what and whether it succeeded.
 *
 * Example:
 *   @Auditable(action = "CREATE_ROUTE", resourceType = "ROUTE")
 *   public RouteResponse createRoute(...) { ... }
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {

    /** Operation name stored in the audit record, e.g. CREATE_ROUTE */
    String action();

    /** Type of resource being acted upon, e.g. ROUTE, ORGANIZATION */
    String resourceType() default "";
}
