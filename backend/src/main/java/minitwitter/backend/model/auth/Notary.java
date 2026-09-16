package minitwitter.backend.model.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;

public class Notary {

    static final String ID_CLAIM_KEY = "id";
    private final SecretKey signatureKey;
    //Esta key no debería esta aca, sino en configuracion del servicio
    private String key = "2DazT1TCBGi4lcctlN7el8buF3n3uO9H8xfKiZWz3no=";

    private Notary() {
        this.signatureKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(key));
    }

    public static Notary notary() {
        return new Notary();
    }

    public String generateTokenFor(int userId) {
        return Jwts.builder()
                .claim(ID_CLAIM_KEY, userId)
                //.expiration(Date.from(LocalDateTime.now().plusHours(1).atZone(java.time.ZoneId.systemDefault()).toInstant()))
                .signWith(signatureKey)
                .compact();
    }

    public int verifyToken(String token) {
        var jwt = Jwts.parser()
                .verifyWith(signatureKey)
                .build()
                .parseSignedClaims(token);
        return (int) jwt.getPayload().get(ID_CLAIM_KEY);
    }
}