package com.gatekeeper.management.mapper;

import com.gatekeeper.management.dto.response.ManagementUserResponse;
import com.gatekeeper.management.entity.ManagementUser;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * Maps ManagementUser entity to its response DTO.
 *
 * role.name  → role        (enum → String)
 * organization.slug → organizationSlug  (null-safe — platform users have no org)
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ManagementUserMapper {

    @Mapping(target = "role",             source = "role.name")
    @Mapping(target = "organizationSlug", source = "organization.slug")
    ManagementUserResponse toResponse(ManagementUser user);
}
