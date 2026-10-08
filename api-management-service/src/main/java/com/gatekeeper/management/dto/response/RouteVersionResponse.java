package com.gatekeeper.management.dto.response;

import com.gatekeeper.management.enums.RouteChangeType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * One version entry in a route's configuration history.
 */
@Data
public class RouteVersionResponse {

    private Long id;
    private String routeId;
    private int versionNumber;
    private RouteChangeType changeType;
    private Map<String, Object> configurationSnapshot;
    private String createdBy;
    private LocalDateTime createdAt;
}
