package com.gxj.cropyield.modules.profile.dto;

public record UserProfileResponse(
    SecuritySettings security,
    BusinessSection business,
    PersonalizationSettings personalization,
    DataOperations dataOperations
) {
}
