package com.gatekeeper.management.audit;

import com.gatekeeper.management.entity.ControlPlaneAuditRecord;
import com.gatekeeper.management.enums.AuditResult;
import com.gatekeeper.management.repository.ControlPlaneAuditRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * Intercepts methods annotated with @Auditable and saves a
 * ControlPlaneAuditRecord for every invocation — regardless of success or failure.
 *
 * What gets captured:
 *   actor          → currently authenticated username
 *   action         → from @Auditable.action()
 *   resourceType   → from @Auditable.resourceType()
 *   resourceId     → first String/Long argument (if any)
 *   organizationId → second Long argument (if any)
 *   durationMs     → wall-clock time of the method
 *   result         → SUCCESS or FAILURE
 *   details        → exception message on failure
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final ControlPlaneAuditRecordRepository auditRepository;

    @Around("@annotation(com.gatekeeper.management.audit.Auditable)")
    public Object audit(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method          method    = signature.getMethod();
        Auditable       auditable = method.getAnnotation(Auditable.class);

        String actor        = currentUsername();
        String action       = auditable.action();
        String resourceType = auditable.resourceType();

        // Try to extract meaningful resource identifiers from the arguments
        Object[] args       = joinPoint.getArgs();
        Long     orgId      = extractLong(args, 0);
        String   resourceId = extractString(args, 1);

        AuditResult         result  = AuditResult.SUCCESS;
        Map<String, Object> details = null;
        Object              returnValue;

        try {
            returnValue = joinPoint.proceed();
        } catch (Throwable ex) {
            result  = AuditResult.FAILURE;
            details = Map.of("error", ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName());
            long duration = System.currentTimeMillis() - start;
            saveAuditRecord(actor, orgId, action, resourceType, resourceId,
                            duration, result, details);
            throw ex;
        }

        long duration = System.currentTimeMillis() - start;
        saveAuditRecord(actor, orgId, action, resourceType, resourceId,
                        duration, result, details);
        return returnValue;
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private void saveAuditRecord(
            String actor, Long orgId, String action,
            String resourceType, String resourceId,
            long durationMs, AuditResult result,
            Map<String, Object> details) {
        try {
            ControlPlaneAuditRecord record = ControlPlaneAuditRecord.builder()
                    .actor(actor)
                    .organizationId(orgId)
                    .action(action)
                    .resourceType(resourceType)
                    .resourceId(resourceId)
                    .durationMs(durationMs)
                    .result(result)
                    .details(details)
                    .build();
            auditRepository.save(record);
        } catch (Exception ex) {
            // Audit failure must never break the main operation
            log.error("Failed to save audit record for action={}: {}", action, ex.getMessage());
        }
    }

    private String currentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }

    private Long extractLong(Object[] args, int index) {
        if (args != null && args.length > index && args[index] instanceof Long l) return l;
        return null;
    }

    private String extractString(Object[] args, int index) {
        if (args != null && args.length > index && args[index] instanceof String s) return s;
        if (args != null && args.length > index && args[index] instanceof Long l)   return String.valueOf(l);
        return null;
    }
}
