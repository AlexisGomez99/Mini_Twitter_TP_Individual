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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 25)
    private String username;

    @Column(nullable = false)
    private String password;

    private LocalDateTime createdAt;

    // "Borrar" un usuario no elimina la fila: un tweet suyo puede seguir siendo
    // el origen de un retweet de otra persona, y esa referencia no debe romperse.
    // En cambio se marca deleted=true y sus tweets propios se ocultan en cascada.
    @Column(nullable = false)
    private boolean deleted;

    @OneToMany(mappedBy = "author")
    private List<Tweet> tweets = new ArrayList<>();

    // Relaciones de Follow donde este usuario es el seguidor/el seguido. A diferencia de
    // los tweets, un follow no tiene valor propio una vez que una de las cuentas
    // desaparece, así que sí se borran de verdad (ver markAsDeleted).
    @OneToMany(mappedBy = "follower", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Follow> followsAsFollower = new ArrayList<>();

    @OneToMany(mappedBy = "followed", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Follow> followsAsFollowed = new ArrayList<>();

    public User(String username, String password) {
        setUsername(username);
        this.password = password;
        this.createdAt = LocalDateTime.now();
    }

    private void setUsername(String username) {
        if (username == null || username.length() < 5 || username.length() > 25) {
            throw new DomainException("El nombre de usuario debe tener entre 5 y 25 caracteres.");
        }
        this.username = username;
    }

    // Soft delete: la fila se conserva para no romper referencias externas (retweets
    // de otros usuarios sobre tweets de este). Sus propios tweets se ocultan en cascada,
    // y sus relaciones de follow (en ambos sentidos) sí se eliminan.
    public void markAsDeleted() {
        this.deleted = true;
        this.tweets.forEach(Tweet::markAsDeleted);
        this.followsAsFollower.clear();
        this.followsAsFollowed.clear();
    }

    public UserInfo toInfo() {
        return new UserInfo(this.id, this.username, this.createdAt);
    }
}