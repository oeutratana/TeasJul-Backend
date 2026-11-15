package com.example.spring_boot_project_api.dto.response;

/**
 * Issued when credentials are correct but two-factor authentication is
 * required. No final JWT is issued until the challenge is verified.
 */
public record TwoFactorChallengeResponse(
        boolean requiresTwoFactor,
        String challengeToken,
        long expiresIn,
        String twoFactorMethod
) {
}
