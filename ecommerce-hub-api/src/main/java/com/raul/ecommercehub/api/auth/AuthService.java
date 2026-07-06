package com.raul.ecommercehub.api.auth;

import com.raul.ecommercehub.shared.domain.RefreshToken;
import com.raul.ecommercehub.shared.domain.Tenant;
import com.raul.ecommercehub.shared.domain.User;
import com.raul.ecommercehub.shared.repository.RefreshTokenRepository;
import com.raul.ecommercehub.shared.repository.TenantRepository;
import com.raul.ecommercehub.shared.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private static final long REFRESH_TOKEN_TTL_DAYS = 7;

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRevocationService refreshTokenService;
    private final JwtService jwtService;
    private final SecureRandom secureRandom = new SecureRandom();
    private final String dummyHash;

    public AuthService(UserRepository userRepository,
                       TenantRepository tenantRepository,
                       RefreshTokenRevocationService refreshTokenService,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.refreshTokenService = refreshTokenService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        // Hash Argon2id de um valor aleatório, gerado pelo próprio encoder —
        // garante formato válido pra comparar contra quando o email não existe.
        this.dummyHash = passwordEncoder.encode("dummy-" + UUID.randomUUID());
    }

    @Transactional
    public TokenPair login(String email, String rawPassword) {
        Optional<User> maybeUser = userRepository.findByEmail(email);

        // Anti-enumeração: SEMPRE roda o Argon2id, mesmo se o email não existir,
        // contra um hash fixo. Sem isso, "email não encontrado" responde mais
        // rápido que "senha errada" — e essa diferença de tempo já é um oráculo.
        String hashToCheck = maybeUser.map(User::getPasswordHash).orElse(dummyHash);
        boolean passwordMatches = passwordEncoder.matches(rawPassword, hashToCheck);

        if (maybeUser.isEmpty() || !passwordMatches || !maybeUser.get().isActive()) {
            throw new InvalidCredentialsException();
        }

        User user = maybeUser.get();
        return issueTokenPair(user.getId(), user.getTenantId());
    }

    @Transactional
    public void register(String tenantName, String cnpj, String email, String rawPassword) {
        Tenant tenant = new Tenant(
                UUID.randomUUID(),
                tenantName,
                cnpj,
                Tenant.Plan.FREE,
                Tenant.TenantStatus.ACTIVE,
                LocalDateTime.now());
        tenantRepository.save(tenant);

        String passwordHash = passwordEncoder.encode(rawPassword);
        User user = new User(
                UUID.randomUUID(),
                tenant.getId(),
                email,
                passwordHash,
                User.Role.ADMIN,
                User.Status.ACTIVE);
        userRepository.save(user);
    }

    @Transactional
    public TokenPair refresh(String rawRefreshToken) {
        String hash = sha256(rawRefreshToken);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(InvalidCredentialsException::new);

        if (stored.isRevoked()) {
            // Reuso de um token já rotacionado = indício de roubo.
            // Revoga TODA a cadeia do usuário, força login novo.
                refreshTokenService.revokeAllTokensForUserInNewTransaction(stored.getUserId());
                throw new RefreshTokenReuseDetectedException();
        }
        if (!stored.isUsable()) {
            throw new InvalidCredentialsException();
        }

        User user = userRepository.findById(stored.getUserId())
                .filter(User::isActive)
                .orElseThrow(InvalidCredentialsException::new);

        TokenPair newPair = issueTokenPair(user.getId(), user.getTenantId());
        stored.markReplacedBy(newPair.refreshTokenId());
        refreshTokenRepository.save(stored);

        return newPair;
    }

    private TokenPair issueTokenPair(UUID userId, UUID tenantId) {
        String accessToken = jwtService.issueAccessToken(userId, tenantId);

        String rawRefreshToken = generateRefreshTokenValue();
        UUID refreshTokenId = UUID.randomUUID();
        RefreshToken refreshToken = new RefreshToken(
                refreshTokenId, userId, sha256(rawRefreshToken),
                LocalDateTime.now().plusDays(REFRESH_TOKEN_TTL_DAYS));
        refreshTokenRepository.save(refreshToken);

        return new TokenPair(accessToken, rawRefreshToken, refreshTokenId);
    }

    private String generateRefreshTokenValue() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(digest.digest(value.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}