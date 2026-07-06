package com.raul.ecommercehub.api.auth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final byte[] secretKey;
    private final long accessTokenTtlMinutes;

    public JwtService(@Value("${security.jwt.secret}") String secret,
                      @Value("${security.jwt.access-token-ttl-minutes}") long accessTokenTtlMinutes) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET não foi configurado — recusando subir sem chave de assinatura");
        }
        this.secretKey = secret.getBytes(StandardCharsets.UTF_8);
        this.accessTokenTtlMinutes = accessTokenTtlMinutes;
    }

    public String issueAccessToken(UUID userId, UUID tenantId) {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(userId.toString())
                .claim("tenant_id", tenantId.toString())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(accessTokenTtlMinutes * 60)))
                .build();

        try {
            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            signedJWT.sign(new MACSigner(secretKey));
            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("Falha ao assinar o JWT", e);
        }
    }

    public JWTClaimsSet validate(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);

            // Regra: só HS256. Nunca confiar no header sem checar.
            if (!JWSAlgorithm.HS256.equals(signedJWT.getHeader().getAlgorithm())) {
                throw new InvalidJwtException("Algoritmo de JWT inesperado: " + signedJWT.getHeader().getAlgorithm());
            }
            if (!signedJWT.verify(new MACVerifier(secretKey))) {
                throw new InvalidJwtException("Assinatura do JWT inválida");
            }

            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            if (claims.getExpirationTime() == null || claims.getExpirationTime().before(new Date())) {
                throw new InvalidJwtException("JWT expirado");
            }
            return claims;
        } catch (ParseException | JOSEException e) {
            throw new InvalidJwtException("JWT malformado", e);
        }
    }
}