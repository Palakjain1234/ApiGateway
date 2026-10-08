package com.gatekeeper.management.enums;

/**
 * Action to perform on a header when a request passes through the Gateway.
 *
 * ADD    → inject the header with the configured value
 * REMOVE → strip the header from the request before forwarding
 */
public enum HeaderAction {
    ADD,
    REMOVE
}
