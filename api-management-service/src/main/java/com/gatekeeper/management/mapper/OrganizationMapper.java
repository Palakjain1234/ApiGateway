package com.gatekeeper.management.mapper;

import com.gatekeeper.management.dto.response.OrganizationResponse;
import com.gatekeeper.management.entity.Organization;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

/**
 * Maps between Organization entity and its response DTO.
 * Spring injects this as a bean automatically (componentModel = "spring").
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface OrganizationMapper {

    OrganizationResponse toResponse(Organization organization);

    List<OrganizationResponse> toResponseList(List<Organization> organizations);
}
