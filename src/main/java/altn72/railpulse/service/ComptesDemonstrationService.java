package altn72.railpulse.service;

import altn72.railpulse.modele.Profil;
import altn72.railpulse.modele.User;
import altn72.railpulse.modele.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ComptesDemonstrationService {
    public static final String LOGIN_AGENT = "demo.agent";
    public static final String LOGIN_RESPONSABLE = "demo.responsable";
    private static final int LONGUEUR_MIN = 12;

    private final UserRepository utilisateurs;
    private final PasswordEncoder encodeur;

    public ComptesDemonstrationService(UserRepository utilisateurs, PasswordEncoder encodeur) {
        this.utilisateurs = utilisateurs;
        this.encodeur = encodeur;
    }

    @Transactional
    public void initialiser(String motDePasseAgent, String motDePasseResponsable) {
        creerSiAbsent(LOGIN_AGENT, "Camille", Profil.AGENT, motDePasseAgent);
        creerSiAbsent(LOGIN_RESPONSABLE, "Alex", Profil.RESPONSABLE_EXPLOITATION, motDePasseResponsable);
    }

    private void creerSiAbsent(String login, String prenom, Profil profil, String motDePasse) {
        User existant = utilisateurs.findByLogin(login).orElse(null);
        if (existant != null) {
            if (existant.getProfil() != profil) {
                throw new IllegalStateException("Un login de démonstration existe déjà avec un autre profil.");
            }
            return;
        }
        if (motDePasse == null || motDePasse.isBlank() || motDePasse.length() < LONGUEUR_MIN) {
            throw new IllegalStateException("Définissez les deux variables RAILPULSE_DEMO_AGENT_PASSWORD et RAILPULSE_DEMO_RESPONSABLE_PASSWORD (12 caractères minimum).");
        }
        utilisateurs.save(new User(login, encodeur.encode(motDePasse), prenom, profil));
    }
}
