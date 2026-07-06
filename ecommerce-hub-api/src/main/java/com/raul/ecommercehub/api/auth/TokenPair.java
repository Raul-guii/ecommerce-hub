package com.raul.ecommercehub.api.auth;

import java.util.UUID;

public record TokenPair(String accessToken, String refreshToken, UUID refreshTokenId) {
}