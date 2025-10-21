package com.gxj.cropyield.modules.profile.dto;

import jakarta.validation.Valid;

public record UpdateUserProfileRequest(
    @Valid SecuritySettings security,
    @Valid BusinessSection business,
    @Valid PersonalizationSettings personalization,
    @Valid DataOperations dataOperations
) {
}
