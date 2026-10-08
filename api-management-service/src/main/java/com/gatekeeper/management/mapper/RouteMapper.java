package com.gatekeeper.management.mapper;

import com.gatekeeper.management.dto.response.ActiveRouteConfigResponse;
import com.gatekeeper.management.dto.response.RouteResponse;
import com.gatekeeper.management.dto.response.RouteVersionResponse;
import com.gatekeeper.management.entity.ApiRoute;
import com.gatekeeper.management.entity.RouteAllowedMethod;
import com.gatekeeper.management.entity.RouteConfigurationVersion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Maps ApiRoute entity and related entities to their response DTOs.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface RouteMapper {

    /**
     * Full route response for TENANT_ADMIN endpoints.
     * allowedMethods is a List<RouteAllowedMethod> → List<String>
     * organizationSlug comes from the nested organization entity.
     */
    @Mapping(target = "allowedMethods",   source = "allowedMethods", qualifiedByName = "methodsToStrings")
    @Mapping(target = "organizationSlug", source = "organization.slug")
    RouteResponse toResponse(ApiRoute route);

    List<RouteResponse> toResponseList(List<ApiRoute> routes);

    /**
     * Lightweight projection for Smart Gateway consumption.
     */
    @Mapping(target = "allowedMethods",   source = "allowedMethods", qualifiedByName = "methodsToStrings")
    @Mapping(target = "organizationSlug", source = "organization.slug")
    ActiveRouteConfigResponse toActiveConfig(ApiRoute route);

    List<ActiveRouteConfigResponse> toActiveConfigList(List<ApiRoute> routes);

    /** Maps a RouteConfigurationVersion entity to its response DTO. */
    RouteVersionResponse toVersionResponse(RouteConfigurationVersion version);

    List<RouteVersionResponse> toVersionResponseList(List<RouteConfigurationVersion> versions);

    /** Extracts just the HTTP method string from a RouteAllowedMethod entity. */
    @Named("methodsToStrings")
    default List<String> methodsToStrings(List<RouteAllowedMethod> methods) {
        if (methods == null) return List.of();
        return methods.stream()
                .map(RouteAllowedMethod::getHttpMethod)
                .collect(Collectors.toList());
    }
}
