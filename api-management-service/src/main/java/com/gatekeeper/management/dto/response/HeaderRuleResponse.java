package com.gatekeeper.management.dto.response;

import com.gatekeeper.management.enums.HeaderAction;
import lombok.Data;

/**
 * One header rule inside a RouteResponse.
 */
@Data
public class HeaderRuleResponse {

    private Long id;
    private HeaderAction action;
    private String headerName;
    private String headerValue;
}
