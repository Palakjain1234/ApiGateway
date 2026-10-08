package com.gatekeeper.management.controller;

import com.gatekeeper.management.dto.request.CreateOrganizationRequest;
import com.gatekeeper.management.dto.request.CreateTenantAdminRequest;
import com.gatekeeper.management.dto.response.ApiResponse;
import com.gatekeeper.management.dto.response.ManagementUserResponse;
import com.gatekeeper.management.dto.response.OrganizationResponse;
import com.gatekeeper.management.service.OrganizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * PLATFORM_ADMIN only — manages organizations (tenants) and their admin accounts.
 *
 * GET    /api/organizations                         → list all organizations
 * GET    /api/organizations/{id}                    → get one organization
 * POST   /api/organizations                         → create organization
 * PUT    /api/organizations/{id}/activate           → activate
 * PUT    /api/organizations/{id}/suspend            → suspend
 * DELETE /api/organizations/{id}                    → soft delete
 * POST   /api/organizations/{id}/admin              → create tenant admin
 */
@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class OrganizationController {

    private final OrganizationService orgService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrganizationResponse>>> getAllOrganizations() {
        return ResponseEntity.ok(ApiResponse.ok(orgService.getAllOrganizations()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrganizationResponse>> getOrganization(
            @PathVariable Long id) {

        return ResponseEntity.ok(ApiResponse.ok(orgService.getOrganizationById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrganizationResponse>> createOrganization(
            @Valid @RequestBody CreateOrganizationRequest request) {

        OrganizationResponse created = orgService.createOrganization(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(created));
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<OrganizationResponse>> activateOrganization(
            @PathVariable Long id) {

        return ResponseEntity.ok(ApiResponse.ok(orgService.activateOrganization(id)));
    }

    @PutMapping("/{id}/suspend")
    public ResponseEntity<ApiResponse<OrganizationResponse>> suspendOrganization(
            @PathVariable Long id) {

        return ResponseEntity.ok(ApiResponse.ok(orgService.suspendOrganization(id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteOrganization(
            @PathVariable Long id) {

        orgService.deleteOrganization(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/{id}/admin")
    public ResponseEntity<ApiResponse<ManagementUserResponse>> createTenantAdmin(
            @PathVariable Long id,
            @Valid @RequestBody CreateTenantAdminRequest request) {

        ManagementUserResponse admin = orgService.createTenantAdmin(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(admin));
    }
}
