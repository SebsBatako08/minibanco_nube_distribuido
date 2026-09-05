import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;

/* public class JwtUtil {
    // Genera una llave criptográfica súper segura en memoria para firmar los tokens
    private static final Key SECRET_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    // El token durará 1 hora (3600000 milisegundos)
    private static final long EXPIRATION_TIME = 3600000; 

    public static String generarToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SECRET_KEY)
                .compact();
    }

    public static boolean validarToken(String token) {
        try {
            // Si el token fue alterado o ya expiró, esto lanzará una excepción
            Jwts.parserBuilder().setSigningKey(SECRET_KEY).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
} */

public class JwtUtil {
    
    // El token maestro fijo que tu equipo ya tiene
    private static final String TOKEN_FIJO = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c3VhcmlvX2NvbmN1cnNvIiwiZXhwIjoxNzgyMjkzMzE4fQ.mfhsaen7NiBAR1wj5otzDLW3l7rXTCmmUzyDcZtgtY0"; 

    public static String generarToken(String username) {
        // Devuelve siempre el token maestro
        return TOKEN_FIJO;
    }

    public static boolean validarToken(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }

        // Limpiamos el token por si mandan la palabra "Bearer "
        String tokenLimpio = token.replace("Bearer ", "").trim();

        // Validamos que sea exactamente igual al fijo
        return tokenLimpio.equals(TOKEN_FIJO);
    }
}