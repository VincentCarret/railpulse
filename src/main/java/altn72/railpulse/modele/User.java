package altn72.railpulse.modele;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    @NotBlank(message = "Le login est obligatoire.")
    private String login;

    @Column(nullable = false)
    @NotBlank(message = "Le mot de passe haché est obligatoire.")
    @Pattern(regexp = FormatMotDePasse.BCRYPT, message = "Le mot de passe doit être haché avec BCrypt.")
    private String password;

    @Column(nullable = false)
    @NotBlank(message = "Le prénom est obligatoire.")
    private String prenom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull(message = "Le profil est obligatoire.")
    private Profil profil;

    public User() {
    }

    public User(String login, String password, String prenom, Profil profil) {
        this.login = login;
        this.password = password;
        this.prenom = prenom;
        this.profil = profil;
    }

    public Long getId() {
        return id;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public Profil getProfil() {
        return profil;
    }

    public void setProfil(Profil profil) {
        this.profil = profil;
    }
}
