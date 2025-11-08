package com.cramer.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

@Component

public class JwtUtil {



    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);



    @Value("${supabase.jwt.secret}")

    private String secret;



        private SecretKey getSigningKey() {



            // The secret key is a plain string, not Base64 encoded.



            // We need to convert it to bytes using UTF-8.



            return Keys.hmacShaKeyFor(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8));



        }



    public String extractUserId(String token) {

        return extractClaim(token, Claims::getSubject);

    }



    public Date extractExpiration(String token) {

        return extractClaim(token, Claims::getExpiration);

    }



    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {

        final Claims claims = extractAllClaims(token);

        return claimsResolver.apply(claims);

    }



    private Claims extractAllClaims(String token) {

        // Simplifying validation to ONLY check the signature

        return Jwts.parser()

                .verifyWith(getSigningKey())

                .build()

                .parseSignedClaims(token)

                .getPayload();

    }



    private Boolean isTokenExpired(String token) {

        return extractExpiration(token).before(new Date());

    }





    

    public Boolean validateToken(String token) {
        try {
            // Jwts.parser will throw an exception if the token is invalid
            extractAllClaims(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }
}
