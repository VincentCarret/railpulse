
package altn72.railpulse.controleur;

import altn72.railpulse.modele.User;
import altn72.railpulse.service.AuthService;

import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalUserControllerAdvice {

    private final AuthService authService;

    public GlobalUserControllerAdvice(AuthService authService) {
        this.authService = authService;
    }

    @ModelAttribute
    public void ajouterUtilisateur(Model model,
                                   Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return;
        }

        User user = authService.getUserByLogin(
                authentication.getName()
        );

        model.addAttribute("prenom", user.getPrenom());
        model.addAttribute("profil", user.getProfil());
    }
}
