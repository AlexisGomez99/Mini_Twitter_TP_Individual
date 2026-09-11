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
    public static final String POSTGRES_URL = "jdbc:postgresql://localhost:5433/minitwitter_db";
    public static final String POSTGRES_USER = "postgres";
    public static final String POSTGRES_PASSWORD = "postgrespassword";
    public static final String POSTGRES_DRIVER = "org.postgresql.Driver";

    public static final String H2_MEMORY_URL = "jdbc:h2:mem:minitwitter;DB_CLOSE_DELAY=-1";
    public static final String H2_USER = "sa";
    public static final String H2_PASSWORD = "";
    public static final String H2_DRIVER = "org.h2.Driver";

    private final PersistenceConfiguration config;

    public EmfBuilder() {
        config = new PersistenceConfiguration("minitwitter")
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
        config.property(PersistenceConfiguration.JDBC_URL, POSTGRES_URL)
                .property(PersistenceConfiguration.JDBC_USER, POSTGRES_USER)
                .property(PersistenceConfiguration.JDBC_PASSWORD, POSTGRES_PASSWORD)
                .property(PersistenceConfiguration.JDBC_DRIVER, POSTGRES_DRIVER);
        return this;
    }

    public EmfBuilder h2Memory() {
        config.property(PersistenceConfiguration.JDBC_URL, H2_MEMORY_URL)
                .property(PersistenceConfiguration.JDBC_USER, H2_USER)
                .property(PersistenceConfiguration.JDBC_PASSWORD, H2_PASSWORD)
                .property(PersistenceConfiguration.JDBC_DRIVER, H2_DRIVER);
        return this;
    }


    public EmfBuilder withUpdateDDL() {
        config.property(AvailableSettings.HBM2DDL_AUTO, Action.UPDATE);
        return this;
    }

    public EmfBuilder withDropAndCreateDDL() {
        config.property(PersistenceConfiguration.SCHEMAGEN_DATABASE_ACTION,
                Action.SPEC_ACTION_DROP_AND_CREATE);
        return this;
    }

    public EntityManagerFactory build() {
        return config.createEntityManagerFactory();
    }
}
