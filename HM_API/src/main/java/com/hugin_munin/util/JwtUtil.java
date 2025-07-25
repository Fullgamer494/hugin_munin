package com.hugin_munin.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hugin_munin.model.Usuario;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Utilidad para manejo de JWT simplificado
 * Implementación básica sin dependencias externas JWT
 */
public class JwtUtil {

    private static final String SECRET_KEY = "HuginMuninSecretKeyForJWTTokensVerySecureAndLong2024!";
    private static final long JWT_EXPIRATION = 30L * 24 * 60 * 60 * 1000; // 30 días
    private static final String ALGORITHM = "HmacSHA256";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Generar token JWT para usuario
     */
    public static String generateToken(Usuario usuario) {
        try {
            // Header
            Map<String, Object> header = new HashMap<>();
            header.put("alg", "HS256");
            header.put("typ", "JWT");

            // Payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("sub", usuario.getNombre_usuario());
            payload.put("id_usuario", usuario.getId_usuario());
            payload.put("nombre_usuario", usuario.getNombre_usuario());
            payload.put("correo", usuario.getCorreo());
            payload.put("id_rol", usuario.getId_rol());
            payload.put("activo", usuario.isActivo());
            payload.put("iat", System.currentTimeMillis() / 1000);
            payload.put("exp", (System.currentTimeMillis() + JWT_EXPIRATION) / 1000);
            payload.put("iss", "HuginMunin");

            // Convertir a JSON y codificar en Base64
            String headerEncoded = base64UrlEncode(objectMapper.writeValueAsString(header));
            String payloadEncoded = base64UrlEncode(objectMapper.writeValueAsString(payload));

            // Crear signature
            String data = headerEncoded + "." + payloadEncoded;
            String signature = createSignature(data);

            return data + "." + signature;

        } catch (Exception e) {
            System.err.println("Error generando token: " + e.getMessage());
            return null;
        }
    }

    /**
     * Extraer claims del token
     */
    public static Map<String, Object> extractClaims(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                return null;
            }

            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                System.err.println("Token JWT inválido: formato incorrecto");
                return null;
            }

            // Verificar signature
            String data = parts[0] + "." + parts[1];
            String expectedSignature = createSignature(data);

            if (!expectedSignature.equals(parts[2])) {
                System.err.println("Token JWT inválido: signature incorrecta");
                return null;
            }

            // Decodificar payload
            String payloadJson = base64UrlDecode(parts[1]);
            @SuppressWarnings("unchecked")
            Map<String, Object> claims = objectMapper.readValue(payloadJson, Map.class);

            return claims;

        } catch (Exception e) {
            System.err.println("Error extrayendo claims: " + e.getMessage());
            return null;
        }
    }

    /**
     * Extraer usuario del token
     */
    public static Usuario extractUsuario(String token) {
        try {
            Map<String, Object> claims = extractClaims(token);
            if (claims == null) {
                return null;
            }

            Usuario usuario = new Usuario();
            usuario.setId_usuario((Integer) claims.get("id_usuario"));
            usuario.setNombre_usuario((String) claims.get("nombre_usuario"));
            usuario.setCorreo((String) claims.get("correo"));
            usuario.setId_rol((Integer) claims.get("id_rol"));
            usuario.setActivo((Boolean) claims.get("activo"));

            return usuario;
        } catch (Exception e) {
            System.err.println("Error extrayendo usuario: " + e.getMessage());
            return null;
        }
    }

    /**
     * Extraer nombre de usuario del token
     */
    public static String extractUsername(String token) {
        Map<String, Object> claims = extractClaims(token);
        return claims != null ? (String) claims.get("sub") : null;
    }

    /**
     * Extraer fecha de expiración
     */
    public static Date extractExpiration(String token) {
        Map<String, Object> claims = extractClaims(token);
        if (claims == null) {
            return null;
        }

        Object exp = claims.get("exp");
        if (exp instanceof Number) {
            long expTime = ((Number) exp).longValue() * 1000; // Convertir a millisegundos
            return new Date(expTime);
        }
        return null;
    }

    /**
     * Verificar si el token ha expirado
     */
    public static Boolean isTokenExpired(String token) {
        Date expiration = extractExpiration(token);
        return expiration != null && expiration.before(new Date());
    }

    /**
     * Validar token con username
     */
    public static Boolean validateToken(String token, String username) {
        try {
            final String tokenUsername = extractUsername(token);
            return (username.equals(tokenUsername) && !isTokenExpired(token));
        } catch (Exception e) {
            System.err.println("Error validando token: " + e.getMessage());
            return false;
        }
    }

    /**
     * Validar token sin comparar username
     */
    public static Boolean validateToken(String token) {
        try {
            return !isTokenExpired(token) && extractClaims(token) != null;
        } catch (Exception e) {
            System.err.println("Error validando token: " + e.getMessage());
            return false;
        }
    }

    /**
     * Obtener tiempo restante del token en milisegundos
     */
    public static long getTimeToExpiration(String token) {
        Date expiration = extractExpiration(token);
        if (expiration == null) {
            return 0;
        }
        return expiration.getTime() - System.currentTimeMillis();
    }

    /**
     * Verificar si el token necesita renovación (falta menos de 7 días)
     */
    public static boolean needsRefresh(String token) {
        long timeToExpiration = getTimeToExpiration(token);
        long sevenDaysInMs = 7L * 24 * 60 * 60 * 1000;
        return timeToExpiration > 0 && timeToExpiration < sevenDaysInMs;
    }

    /**
     * Refrescar token (generar nuevo token para el mismo usuario)
     */
    public static String refreshToken(String token) {
        Usuario usuario = extractUsuario(token);
        if (usuario != null && validateToken(token)) {
            return generateToken(usuario);
        }
        return null;
    }

    /**
     * Crear signature HMAC SHA256
     */
    private static String createSignature(String data) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    SECRET_KEY.getBytes(StandardCharsets.UTF_8),
                    ALGORITHM
            );
            mac.init(secretKeySpec);

            byte[] signature = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return base64UrlEncode(signature);

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Error creando signature", e);
        }
    }

    /**
     * Codificar en Base64 URL-safe
     */
    private static String base64UrlEncode(String input) {
        return base64UrlEncode(input.getBytes(StandardCharsets.UTF_8));
    }

    private static String base64UrlEncode(byte[] input) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(input);
    }

    /**
     * Decodificar de Base64 URL-safe
     */
    private static String base64UrlDecode(String input) {
        byte[] decoded = Base64.getUrlDecoder().decode(input);
        return new String(decoded, StandardCharsets.UTF_8);
    }
}