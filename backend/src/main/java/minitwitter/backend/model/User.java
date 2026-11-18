package minitwitter.backend.model;

import minitwitter.backend.dto.UserInfo;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    public static final int LARGO_MINIMO_NOMBRE_USUARIO = 5;
    public static final int LARGO_MAXIMO_NOMBRE_USUARIO = 25;
    public static final String ERROR_NOMBRE_USUARIO_INVALIDO = "El nombre de usuario debe tener entre "
            + LARGO_MINIMO_NOMBRE_USUARIO + " y " + LARGO_MAXIMO_NOMBRE_USUARIO + " caracteres.";
    public static final String ERROR_CONTRASENIA_OBLIGATORIA = "La contraseña es obligatoria.";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = LARGO_MAXIMO_NOMBRE_USUARIO)
    private String username;

    @Column(nullable = false)
    private String password;

    private LocalDateTime createdAt;

    @Column(nullable = false)
    private boolean deleted;

    @OneToMany(mappedBy = "author")
    private List<Tweet> tweets = new ArrayList<>();

    @OneToMany(mappedBy = "follower", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Follow> followsAsFollower = new ArrayList<>();

    @OneToMany(mappedBy = "followed", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Follow> followsAsFollowed = new ArrayList<>();

    public User(String username, String password) {
        setUsername(username);
        if (password == null || password.isBlank()) {
            throw new DomainException(ERROR_CONTRASENIA_OBLIGATORIA);
        }
        //encriptar antes de guardar
        this.password = password;
        this.createdAt = LocalDateTime.now();
    }

    private void setUsername(String username) {
        if (username == null
                || username.length() < LARGO_MINIMO_NOMBRE_USUARIO
                || username.length() > LARGO_MAXIMO_NOMBRE_USUARIO) {
            throw new DomainException(ERROR_NOMBRE_USUARIO_INVALIDO);
        }
        this.username = username;
    }

    // Soft delete
    public void markAsDeleted() {
        this.deleted = true;
        this.tweets.forEach(Tweet::markAsDeleted);
        this.followsAsFollower.clear();
        this.followsAsFollowed.clear();
    }

    public UserInfo toInfo() {
        return new UserInfo(this.id, this.username, this.createdAt);
    }

    public int identificador() {
        return id;
    }
}