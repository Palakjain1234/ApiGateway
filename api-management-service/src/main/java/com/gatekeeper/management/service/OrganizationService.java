package com.gatekeeper.management.service;

import com.gatekeeper.management.audit.Auditable;
import com.gatekeeper.management.dto.request.CreateOrganizationRequest;
import com.gatekeeper.management.dto.request.CreateTenantAdminRequest;
import com.gatekeeper.management.dto.response.ManagementUserResponse;
import com.gatekeeper.management.dto.response.OrganizationResponse;
import com.gatekeeper.management.entity.ManagementUser;
import com.gatekeeper.management.entity.Organization;
import com.gatekeeper.management.entity.Role;
import com.gatekeeper.management.enums.OrganizationStatus;
import com.gatekeeper.management.enums.RoleName;
import com.gatekeeper.management.exception.BadRequestException;
import com.gatekeeper.management.exception.ConflictException;
import com.gatekeeper.management.exception.ResourceNotFoundException;
import com.gatekeeper.management.mapper.ManagementUserMapper;
import com.gatekeeper.management.mapper.OrganizationMapper;
import com.gatekeeper.management.repository.ManagementUserRepository;
import com.gatekeeper.management.repository.OrganizationRepository;
import com.gatekeeper.management.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Business logic for organization (tenant) management.
 * All write operations are PLATFORM_ADMIN only — enforced at the controller level.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationService {

    private final OrganizationRepository   orgRepository;
    private final ManagementUserRepository userRepository;
    private final RoleRepository           roleRepository;
    private final PasswordEncoder          passwordEncoder;
    private final OrganizationMapper       orgMapper;
    private final ManagementUserMapper     userMapper;

    // ── Read operations ──────────────────────────────────────────────────────

    public List<OrganizationResponse> getAllOrganizations() {
        return orgMapper.toResponseList(orgRepository.findAllActive());
    }

    public OrganizationResponse getOrganizationById(Long id) {
        Organization org = findOrThrow(id);
        return orgMapper.toResponse(org);
    }

    public OrganizationResponse getOrganizationBySlug(String slug) {
        Organization org = orgRepository.findBySlug(slug)
                .orElseThrow(() -> ResourceNotFoundException.organizationBySlug(slug));
        return orgMapper.toResponse(org);
    }

    // ── Write operations (PLATFORM_ADMIN only) ───────────────────────────────

    @Transactional
    @Auditable(action = "CREATE_ORGANIZATION", resourceType = "ORGANIZATION")
    public OrganizationResponse createOrganization(CreateOrganizationRequest request) {
        if (orgRepository.existsBySlug(request.getSlug())) {
            throw new ConflictException(
                "An organization with slug '" + request.getSlug() + "' already exists");
        }

        Organization org = Organization.builder()
                .name(request.getName())
                .slug(request.getSlug())
                .status(OrganizationStatus.PENDING)
                .build();

        Organization saved = orgRepository.save(org);
        log.info("Organization created: {} (slug={})", saved.getName(), saved.getSlug());
        return orgMapper.toResponse(saved);
    }

    @Transactional
    @Auditable(action = "ACTIVATE_ORGANIZATION", resourceType = "ORGANIZATION")
    public OrganizationResponse activateOrganization(Long id) {
        Organization org = findOrThrow(id);
        if (org.getStatus() == OrganizationStatus.DELETED) {
            throw new BadRequestException("Cannot activate a deleted organization");
        }
        org.setStatus(OrganizationStatus.ACTIVE);
        return orgMapper.toResponse(orgRepository.save(org));
    }

    @Transactional
    @Auditable(action = "SUSPEND_ORGANIZATION", resourceType = "ORGANIZATION")
    public OrganizationResponse suspendOrganization(Long id) {
        Organization org = findOrThrow(id);
        if (org.getStatus() == OrganizationStatus.DELETED) {
            throw new BadRequestException("Cannot suspend a deleted organization");
        }
        org.setStatus(OrganizationStatus.SUSPENDED);
        return orgMapper.toResponse(orgRepository.save(org));
    }

    @Transactional
    @Auditable(action = "DELETE_ORGANIZATION", resourceType = "ORGANIZATION")
    public void deleteOrganization(Long id) {
        Organization org = findOrThrow(id);
        // Soft delete — preserve data for historical purposes
        org.setStatus(OrganizationStatus.DELETED);
        org.setDeletedAt(LocalDateTime.now());
        orgRepository.save(org);
        log.info("Organization soft-deleted: {} (id={})", org.getName(), org.getId());
    }

    // ── Tenant admin provisioning ────────────────────────────────────────────

    /**
     * Creates the one TENANT_ADMIN account for an organization.
     * Enforces the one-admin-per-org rule.
     */
    @Transactional
    @Auditable(action = "CREATE_TENANT_ADMIN", resourceType = "MANAGEMENT_USER")
    public ManagementUserResponse createTenantAdmin(Long orgId, CreateTenantAdminRequest request) {
        Organization org = findOrThrow(orgId);

        if (userRepository.existsTenantAdminForOrganization(orgId)) {
            throw new ConflictException(
                "Organization '" + org.getSlug() + "' already has a tenant admin");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException(
                "Username '" + request.getUsername() + "' is already taken");
        }

        Role tenantAdminRole = roleRepository.findByName(RoleName.TENANT_ADMIN)
                .orElseThrow(() -> new IllegalStateException("TENANT_ADMIN role not found — check Flyway seed"));

        ManagementUser admin = ManagementUser.builder()
                .organization(org)
                .role(tenantAdminRole)
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .enabled(true)
                .build();

        ManagementUser saved = userRepository.save(admin);
        log.info("Tenant admin created: {} for org: {}", saved.getUsername(), org.getSlug());
        return userMapper.toResponse(saved);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private Organization findOrThrow(Long id) {
        return orgRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.organization(id));
    }
}
