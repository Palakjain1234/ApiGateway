package com.gatekeeper.management.dto.response;

import com.gatekeeper.management.enums.OrganizationStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Returned by organization endpoints.
 * Never exposes internal database IDs to the outside.
 */
@Data
public class OrganizationResponse {

    private Long id;
    private String name;
    private String slug;
    private OrganizationStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
