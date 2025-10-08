package com.dali.cropyield.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRequest(
        @NotBlank @Size(max = 64) String username,
        @NotBlank @Size(min = 6, max = 128) String password,
        @Size(max = 64) String nickname,
        @Email @Size(max = 128) String email) {
}
