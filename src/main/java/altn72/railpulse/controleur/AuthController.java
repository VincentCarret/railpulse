
package altn72.railpulse.controleur;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {

    @GetMapping("/login")
    public String login() {
        return "connexion";
    }

    @GetMapping("/")
    public String accueil() {
        return "accueil";
    }
}
