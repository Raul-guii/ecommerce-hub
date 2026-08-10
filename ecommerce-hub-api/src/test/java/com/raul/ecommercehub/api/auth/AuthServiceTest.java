package com.raul.ecommercehub.api.auth;

import com.nimbusds.jwt.JWTClaimsSet;
import com.raul.ecommercehub.shared.domain.RefreshToken;
import com.raul.ecommercehub.shared.domain.Tenant;
import com.raul.ecommercehub.shared.domain.User;
import com.raul.ecommercehub.shared.repository.RefreshTokenRepository;
import com.raul.ecommercehub.shared.repository.TenantRepository;
import com.raul.ecommercehub.shared.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Cobre o AuthService: registro, login (incluindo anti-enumeração e usuário
 * inativo), e o ciclo de refresh token (rotação normal e detecção de reuso
 * com revogação em cadeia).
 * <p>
 * NÃO cobre: a diferença de tempo de resposta entre "email não existe" e
 * "senha errada" (o próprio propósito do hash dummy) — isso exigiria
 * medição estatística de timing, frágil demais pra CI. O comportamento
 * funcional (mesma exceção genérica nos dois casos) é testado; a garantia
 * de tempo constante fica como verificação manual/carga.
 */
@SpringBootTest
@Testcontainers
@Transactional
class AuthServiceTest {

    @Container
    static MySQLContainer mysql = new MySQLContainer("mysql:8.0");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired
    AuthService authService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    TenantRepository tenantRepository;

    @Autowired
    RefreshTokenRepository refreshTokenRepository;

    @Autowired
    JwtService jwtService;

    @PersistenceContext
    EntityManager entityManager;

    private static final AtomicInteger SEQUENCE = new AtomicInteger(0);

    private String uniqueEmail() {
        return "user" + SEQUENCE.incrementAndGet() + "@teste.com";
    }

    private String uniqueCnpj() {
        return String.format("%014d", SEQUENCE.incrementAndGet());
    }

    private User registerAndFetchUser(String email, String rawPassword) {
        authService.register("Loja Teste", uniqueCnpj(), email, rawPassword);
        return userRepository.findByEmail(email).orElseThrow();
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(digest.digest(value.getBytes()));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void register_criaTenantEUsuarioComSenhaHasheada() {
        String email = uniqueEmail();

        authService.register("Loja Teste", uniqueCnpj(), email, "senha123");

        User user = userRepository.findByEmail(email).orElseThrow();
        assertThat(user.getRole()).isEqualTo(User.Role.ADMIN);
        assertThat(user.isActive()).isTrue();
        // Senha nunca em texto puro — hash Argon2id sempre bem maior e com
        // o prefixo do algoritmo.
        assertThat(user.getPasswordHash()).isNotEqualTo("senha123");
        assertThat(user.getPasswordHash()).startsWith("$argon2id$");

        Tenant tenant = tenantRepository.findById(user.getTenantId()).orElseThrow();
        assertThat(tenant.getName()).isEqualTo("Loja Teste");
        assertThat(tenant.getStatus()).isEqualTo(Tenant.TenantStatus.ACTIVE);
    }

    @Test
    void login_comCredenciaisValidas_retornaTokenPairValido() {
        String email = uniqueEmail();
        User user = registerAndFetchUser(email, "senha123");

        TokenPair pair = authService.login(email, "senha123");

        assertThat(pair.accessToken()).isNotBlank();
        assertThat(pair.refreshToken()).isNotBlank();

        JWTClaimsSet claims = jwtService.validate(pair.accessToken());
        assertThat(claims.getSubject()).isEqualTo(user.getId().toString());
        assertThat(claims.getClaim("tenant_id")).isEqualTo(user.getTenantId().toString());

        RefreshToken stored = refreshTokenRepository.findByTokenHash(sha256(pair.refreshToken()))
                .orElseThrow();
        assertThat(stored.getUserId()).isEqualTo(user.getId());
        assertThat(stored.isRevoked()).isFalse();
    }

    @Test
    void login_comSenhaErrada_lancaInvalidCredentialsException() {
        String email = uniqueEmail();
        registerAndFetchUser(email, "senha123");

        assertThatThrownBy(() -> authService.login(email, "senha-errada"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_comEmailInexistente_lancaMesmaExcecaoQueSenhaErrada() {
        // Anti-enumeração: mesmo erro genérico, exista ou não o email.
        assertThatThrownBy(() -> authService.login("nao-existe@teste.com", "qualquer-senha"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_comUsuarioInativo_lancaInvalidCredentialsException() {
        String email = uniqueEmail();
        User user = registerAndFetchUser(email, "senha123");

        // User não expõe setter de status — SQL nativo, mesmo padrão já
        // usado no projeto pra contornar gaps de mutabilidade das entidades.
        entityManager.createNativeQuery("UPDATE users SET status = 'DISABLED' WHERE id = ?")
                .setParameter(1, user.getId().toString())
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();

        assertThatThrownBy(() -> authService.login(email, "senha123"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void refresh_comTokenValido_rotacionaERevogaOAntigo() {
        String email = uniqueEmail();
        registerAndFetchUser(email, "senha123");
        TokenPair original = authService.login(email, "senha123");

        TokenPair rotated = authService.refresh(original.refreshToken());

        assertThat(rotated.refreshToken()).isNotEqualTo(original.refreshToken());

        RefreshToken oldStored = refreshTokenRepository.findByTokenHash(sha256(original.refreshToken()))
                .orElseThrow();
        assertThat(oldStored.isRevoked()).isTrue();

        RefreshToken newStored = refreshTokenRepository.findByTokenHash(sha256(rotated.refreshToken()))
                .orElseThrow();
        assertThat(newStored.isRevoked()).isFalse();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void refresh_comTokenJaRevogado_detectaReuseERevogaCadeiaInteira() {
        String email = uniqueEmail();
        User user = registerAndFetchUser(email, "senha123");
        TokenPair original = authService.login(email, "senha123");

        TokenPair rotated = authService.refresh(original.refreshToken());

        assertThatThrownBy(() -> authService.refresh(original.refreshToken()))
                .isInstanceOf(RefreshTokenReuseDetectedException.class);

        List<RefreshToken> allTokens = refreshTokenRepository.findAllByUserId(user.getId());
        assertThat(allTokens).allMatch(RefreshToken::isRevoked);

        assertThatThrownBy(() -> authService.refresh(rotated.refreshToken()))
                .isInstanceOf(RefreshTokenReuseDetectedException.class);
    }

    @Test
    void refresh_comTokenInexistente_lancaInvalidCredentialsException() {
        assertThatThrownBy(() -> authService.refresh("valor-que-nunca-existiu"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void refresh_comTokenExpirado_lancaInvalidCredentialsException() {
        String email = uniqueEmail();
        User user = registerAndFetchUser(email, "senha123");

        String rawExpiredToken = "expired-raw-token-value";
        RefreshToken expired = new RefreshToken(
                UUID.randomUUID(), user.getId(), sha256(rawExpiredToken),
                LocalDateTime.now().minusDays(1)); // já expirado
        refreshTokenRepository.save(expired);

        assertThatThrownBy(() -> authService.refresh(rawExpiredToken))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}