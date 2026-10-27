package altn72.railpulse.service;

import altn72.railpulse.modele.FormatMotDePasse;
import altn72.railpulse.modele.User;
import altn72.railpulse.modele.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MigrationMotsDePasse {
    private final UserRepository utilisateurs;
    private final PasswordEncoder encodeur;

    public MigrationMotsDePasse(UserRepository utilisateurs, PasswordEncoder encodeur) {
        this.utilisateurs = utilisateurs;
        this.encodeur = encodeur;
    }

    @Transactional
    public void migrer() {
        for (User utilisateur : utilisateurs.findAllByOrderByIdAsc()) {
            String valeur = utilisateur.getPassword();
            if (FormatMotDePasse.estBcrypt(valeur)) {
                utilisateur.setPassword(FormatMotDePasse.normaliser(valeur));
            } else {
                utilisateur.setPassword(encodeur.encode(ancienMotDePasse(valeur)));
            }
        }
    }

    private String ancienMotDePasse(String valeur) {
        if (valeur != null && valeur.startsWith("{noop}")) {
            valeur = valeur.substring("{noop}".length());
        } else if (valeur != null && (valeur.startsWith("{") || valeur.startsWith("$2"))) {
            throw new IllegalStateException("Format de mot de passe historique inconnu : une réinitialisation est nécessaire.");
        }
        if (valeur == null || valeur.isBlank()) {
            throw new IllegalStateException("Un ancien compte possède un mot de passe vide : une réinitialisation est nécessaire.");
        }
        return valeur;
    }
}
