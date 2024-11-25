package com.devaxiom.pos.security;

import com.devaxiom.pos.exceptions.KeyGenerationException;
import com.devaxiom.pos.model.Users;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cglib.core.internal.Function;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
//public class JWTService implements CommandLineRunner {
public class JwtService {

    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    private final String jwtSecretKey;
    private final String refreshSecretKey;
    private final TokenBlacklistService tokenBlacklistService;

    @Value("${jwt.expiry}")
    private long jwtExpiry;

//    @Value("${jwt.secret-key}")
//    private String jwtSecretKey;

    public JwtService(TokenBlacklistService tokenBlacklistService) throws NoSuchAlgorithmException {
        this.tokenBlacklistService = tokenBlacklistService;

        KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA512");
        SecretKey secretKey = keyGen.generateKey();

        this.jwtSecretKey = Base64.getEncoder().encodeToString(secretKey.getEncoded());
        System.out.println("JWT Secret Key: " + jwtSecretKey);
        this.refreshSecretKey = Base64.getEncoder().encodeToString(secretKey.getEncoded());
        System.out.println("Refresh Secret Key: " + refreshSecretKey);
    }

    public String generateRefreshToken(String userEmail) {
        long REFRESH_EXPIRATION_MS = 1000 * 60 * 60 * 24 * 7;
        logger.debug("Generating refresh token for user: {}", userEmail);
        return generateRefreshToken(userEmail, REFRESH_EXPIRATION_MS);
    }

    public String generateJwtToken(Users user) {
        try {
            Map<String, Object> claims = new HashMap<>();
            claims.put("roles", user.getRole());

            logger.debug("JWT Token creation: subject={}, expirationMs={}", user.getEmail(), jwtExpiry);
            return Jwts.builder()
                    .claims()
                    .issuer("devAxiom")
                    .add(claims)
                    .subject(user.getEmail())
                    .issuedAt(new Date(System.currentTimeMillis()))
                    .expiration(new Date(System.currentTimeMillis() + jwtExpiry))
                    .and()
                    .signWith(getJwtSigningKey())
                    .compact();
        } catch (WeakKeyException e) {
            logger.error("JWT Token creation failed due to weak key: {}", e.getMessage());
            throw new KeyGenerationException("Weak key for HS512 algorithm. Key must be >= 512 bits.", e);
        }
    }

    public String generateRefreshToken(String userEmail, long expirationMs) {
        Map<String, Object> claims = new HashMap<>();
        logger.debug("Refresh Token creation: subject={}, expirationMs={}", userEmail, expirationMs);
        return Jwts.builder()
                .claims()
                .add(claims)
                .subject(userEmail)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .and()
                .signWith(getRefreshSigningKey())
                .compact();
    }

    private SecretKey getJwtSigningKey() {
        logger.debug("Retrieving signing key.");
        return Keys.hmacShaKeyFor(Base64.getDecoder().decode(jwtSecretKey));
    }

    private SecretKey getRefreshSigningKey() {
        logger.debug("Retrieving signing key.");
        return Keys.hmacShaKeyFor(Base64.getDecoder().decode(refreshSecretKey));
    }

    public boolean validateJwtToken(String token, UserDetails userDetails) {
        try {
            final String userName = extractUserNameFromJwt(token);
            logger.debug("Validating JWT token: token={}, userName={}, userDetails={}", token, userName, userDetails.getUsername());
            if (!userName.equals(userDetails.getUsername())) {
                logger.warn("JWT Token validation failed: Username mismatch. Expected {}, found {}", userDetails.getUsername(), userName);
                return false;
            }

            if (isTokenExpired(token, getJwtSigningKey())) {
                logger.warn("JWT Token validation failed: Token is expired.");
                return false;
            }

            if (tokenBlacklistService.isTokenBlacklisted(token)) {
                logger.warn("JWT Token validation failed: Token is blacklisted.");
                return false;
            }

            logger.info("JWT Token is valid.");
            return true;
        } catch (io.jsonwebtoken.security.SignatureException ex) {
            logger.error("JWT Signature does not match locally computed signature. Token should not be trusted. Error: {}", ex.getMessage());
            return false;
        } catch (Exception ex) {
            logger.error("Error during JWT token validation: {}", ex.getMessage());
            return false;
        }
    }

    public boolean validateRefreshToken(String token, UserDetails userDetails) {
        System.out.println("Validating Refresh token");
        final String userName = extractUserNameFromRefreshToken(token);
        logger.debug("Validating Refresh token: token={}, userName={}, userDetails={}", token, userName, userDetails.getUsername());
        return (userName.equals(userDetails.getUsername()) && !isTokenExpired(token, getRefreshSigningKey()) && !tokenBlacklistService.isTokenBlacklisted(token));
    }

    public String extractUserNameFromJwt(String token) {
        logger.debug("Extracting user name from JWT token: {}", token);
        return extractClaim(token, Claims::getSubject, getJwtSigningKey());
    }

    public String extractUserNameFromRefreshToken(String token) {
        logger.debug("Extracting user name from Refresh token: {}", token);
        return extractClaim(token, Claims::getSubject, getRefreshSigningKey());
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimResolver, SecretKey key) {
        final Claims claims = extractAllClaims(token, key);
        return claimResolver.apply(claims);
    }

    private Claims extractAllClaims(String token, SecretKey key) {
        logger.debug("Extracting all claims from token: {}", token);
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isTokenExpired(String token, SecretKey key) {
        Date expiration = extractExpiration(token, key);
        boolean expired = expiration.before(new Date(System.currentTimeMillis() - 1000));
        logger.debug("Token expiration check: token={}, expired={}", token, expired);
        return expired;
    }

    private Date extractExpiration(String token, SecretKey key) {
        logger.debug("Extracting expiration date from token: {}", token);
        return extractClaim(token, Claims::getExpiration, key);
    }

    public String generatePasswordResetToken(Users user) {
        long RESET_TOKEN_EXPIRATION_MS = 1000 * 60 * 30;
        logger.debug("Generating password reset token for user: {}", user.getEmail());
        return generateJwtToken(user);
    }

//    @Override
//    public void run(String... args) {
//        logger.info("JWT Service started.");
//        Map<String, Object> map = new HashMap<>();
//        map.put("name", "John Doe");
//        map.put("email", "123456543@gmail.com");
//        map.put("role", "ADMIN");
//        map.put("phone", "1234567890");
//        System.out.println(generateJwtToken("1234rtygfdsasdfgbh@gmail.com","ADMIN"));
//    }
}
