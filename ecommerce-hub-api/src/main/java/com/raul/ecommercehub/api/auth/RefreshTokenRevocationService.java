package com.raul.ecommercehub.api.auth;

import com.raul.ecommercehub.shared.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class RefreshTokenRevocationService {

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenRevocationService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revokeAllTokensForUserInNewTransaction(UUID userId) {
        refreshTokenRepository.findAllByUserId(userId).forEach(token -> {
            token.markRevoked();
            refreshTokenRepository.save(token);
        });
    }
}
