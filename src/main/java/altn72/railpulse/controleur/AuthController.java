
package altn72.railpulse.controleur;

import altn72.railpulse.modele.User;
import altn72.railpulse.service.AuthService;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/login")
    public String login() {
        return "connexion";
    }

    @GetMapping("/")
    public String accueil(Authentication authentication, Model model) {

        User user = authService.getUserByLogin(
                authentication.getName()
        );

        model.addAttribute("prenom", user.getPrenom());
        model.addAttribute("profil", user.getProfil());

        return "accueil";
    }
}
