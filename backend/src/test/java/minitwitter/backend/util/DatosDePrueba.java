package minitwitter.backend.util;

import jakarta.persistence.EntityManagerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

// Escenario inicial compartido por los tests de integración. Los datos están en
// src/test/resources/test-data.sql y esta clase los carga y ayuda a encontrarlos.
public final class DatosDePrueba {

    public static final String AGOMEZ = "agomez";
    public static final String CONTRASENIA_AGOMEZ = "password123";
    public static final String JPEREZ = "jperez";
    public static final String CONTRASENIA_JPEREZ = "password456";
    public static final String MLOPEZ = "mlopez";
    public static final String CONTRASENIA_MLOPEZ = "password789";

    public static final String TWEET_DE_AGOMEZ = "Primer tweet de Ana";
    public static final String TWEET_DE_JPEREZ = "Tweet de Luis";
    public static final String TWEET_DE_MLOPEZ = "Tweet de María";

    // Id que no corresponde a ningún registro del escenario.
    public static final Integer ID_INEXISTENTE = 999999;

    private static final String ARCHIVO_SQL = "/test-data.sql";

    private DatosDePrueba() {
    }

    // Ejecuta todas las sentencias de test-data.sql en una sola transacción.
    // Se llama después de vaciar las tablas: emf.getSchemaManager().truncate().
    public static void cargar(EntityManagerFactory emf) {
        List<String> sentencias = leerSentencias();
        emf.runInTransaction(em -> sentencias.forEach(sentencia -> em.createNativeQuery(sentencia).executeUpdate()));
    }

    public static Integer idUsuario(EntityManagerFactory emf, String nombreUsuario) {
        return emf.callInTransaction(em -> em
                .createQuery("select u.id from User u where u.username = :nombre", Integer.class)
                .setParameter("nombre", nombreUsuario)
                .getSingleResult());
    }

    public static Integer idTweet(EntityManagerFactory emf, String contenido) {
        return emf.callInTransaction(em -> em
                .createQuery("select o.id from OriginalTweet o where o.content = :contenido", Integer.class)
                .setParameter("contenido", contenido)
                .getSingleResult());
    }

    // Id del primer retweet del usuario dado (en el escenario inicial solo agomez tiene uno).
    public static Integer idRetweetDe(EntityManagerFactory emf, String nombreUsuario) {
        return emf.callInTransaction(em -> em
                .createQuery("select r.id from Retweet r where r.author.username = :nombre", Integer.class)
                .setParameter("nombre", nombreUsuario)
                .getResultList()
                .get(0));
    }

    private static List<String> leerSentencias() {
        try (InputStream entrada = DatosDePrueba.class.getResourceAsStream(ARCHIVO_SQL)) {
            if (entrada == null) {
                throw new IllegalStateException("No se encontró " + ARCHIVO_SQL + " en src/test/resources");
            }
            String contenido = new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
            // Se descartan las líneas de comentario y se separa por ";" (el archivo no usa ";" dentro de textos).
            String sinComentarios = String.join("\n", Arrays.stream(contenido.split("\n"))
                    .filter(linea -> !linea.trim().startsWith("--"))
                    .toList());
            return Arrays.stream(sinComentarios.split(";"))
                    .map(String::trim)
                    .filter(sentencia -> !sentencia.isEmpty())
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer " + ARCHIVO_SQL, e);
        }
    }
}
