package minitwitter.backend.util;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceConfiguration;
import minitwitter.backend.model.Follow;
import minitwitter.backend.model.OriginalTweet;
import minitwitter.backend.model.Retweet;
import minitwitter.backend.model.Tweet;
import minitwitter.backend.model.User;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.tool.schema.Action;


public class EmfBuilder {
    public static final String URL_POSTGRES = "jdbc:postgresql://localhost:5433/minitwitter_db";
    public static final String USUARIO_POSTGRES = "postgres";
    public static final String CONTRASENIA_POSTGRES = "postgrespassword";
    public static final String DRIVER_POSTGRES = "org.postgresql.Driver";

    public static final String URL_H2_EN_MEMORIA = "jdbc:h2:mem:minitwitter;DB_CLOSE_DELAY=-1";
    public static final String USUARIO_H2 = "sa";
    public static final String CONTRASENIA_H2 = "";
    public static final String DRIVER_H2 = "org.h2.Driver";

    private final PersistenceConfiguration configuracion;

    public EmfBuilder() {
        configuracion = new PersistenceConfiguration("minitwitter")
                .managedClass(User.class)
                .managedClass(Tweet.class)
                .managedClass(OriginalTweet.class)
                .managedClass(Retweet.class)
                .managedClass(Follow.class)
                .property(JdbcSettings.SHOW_SQL, true)
                .property(JdbcSettings.FORMAT_SQL, true)
                .property(PersistenceConfiguration.SCHEMAGEN_DATABASE_ACTION, Action.SPEC_ACTION_NONE);
    }

    public EmfBuilder postgres() {
        configuracion.property(PersistenceConfiguration.JDBC_URL, URL_POSTGRES)
                .property(PersistenceConfiguration.JDBC_USER, USUARIO_POSTGRES)
                .property(PersistenceConfiguration.JDBC_PASSWORD, CONTRASENIA_POSTGRES)
                .property(PersistenceConfiguration.JDBC_DRIVER, DRIVER_POSTGRES);
        return this;
    }

    public EmfBuilder h2EnMemoria() {
        configuracion.property(PersistenceConfiguration.JDBC_URL, URL_H2_EN_MEMORIA)
                .property(PersistenceConfiguration.JDBC_USER, USUARIO_H2)
                .property(PersistenceConfiguration.JDBC_PASSWORD, CONTRASENIA_H2)
                .property(PersistenceConfiguration.JDBC_DRIVER, DRIVER_H2);
        return this;
    }


    public EmfBuilder conActualizacionDeEsquema() {
        configuracion.property(AvailableSettings.HBM2DDL_AUTO, Action.UPDATE);
        return this;
    }

    public EmfBuilder conRecreacionDeEsquema() {
        configuracion.property(PersistenceConfiguration.SCHEMAGEN_DATABASE_ACTION,
                Action.SPEC_ACTION_DROP_AND_CREATE);
        return this;
    }

    public EntityManagerFactory construir() {
        return configuracion.createEntityManagerFactory();
    }
}
