package minitwitter.backend.model;

import minitwitter.backend.exception.DomainException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 25)
    private String username;

    @Column(nullable = false)
    private String password;

    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Tweet> tweets = new ArrayList<>();

    public User(String username, String password) {
        setUsername(username);
        this.password = password;
        this.createdAt = LocalDateTime.now();
    }

    public void setUsername(String username) {
        if (username == null || username.length() < 5 || username.length() > 25) {
            throw new DomainException("El nombre de usuario debe tener entre 5 y 25 caracteres.");
        }
        this.username = username;
    }
}