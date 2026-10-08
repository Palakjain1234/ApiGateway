package com.gatekeeper.management.dto.request;

import com.gatekeeper.management.enums.HeaderAction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * One header rule inside a CreateRouteRequest or UpdateRouteRequest.
 */
@Data
public class HeaderRuleRequest {

    @NotNull(message = "Header action is required (ADD or REMOVE)")
    private HeaderAction action;

    @NotBlank(message = "Header name is required")
    @Size(max = 200)
    private String headerName;

    /** Required when action = ADD. Should be null when action = REMOVE. */
    @Size(max = 500)
    private String headerValue;
}
