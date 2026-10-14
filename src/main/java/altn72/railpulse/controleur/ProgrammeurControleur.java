package altn72.railpulse.controleur;

import altn72.railpulse.modele.Programmeur;
import altn72.railpulse.service.ProgrammeurService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/programmeurs")
public class ProgrammeurControleur {
    private final ProgrammeurService programmeurService;

    public ProgrammeurControleur(ProgrammeurService programmeurService) {
        this.programmeurService = programmeurService;
    }

    @InitBinder
    public void limiterChampsModifiables(WebDataBinder binder) {
        binder.setAllowedFields("nom", "prenom", "adresse", "langageExpertise", "livrePrefere", "salaire");
    }

    @GetMapping
    public String afficherInfosProgrammeurs(Model model) {
        model.addAttribute("programmeurs", programmeurService.getProgrammeurs());
        return "listeProgrammeurs";
    }

    @GetMapping("/{idProgrammeur}")
    public String afficherUnProgrammeur(@PathVariable Integer idProgrammeur, Model model) {
        model.addAttribute("programmeurToUpdate", programmeurService.getUnProgrammeur(idProgrammeur));
        return "detailsProgrammeur";
    }

    @GetMapping("/nouveau")
    public String preparerAjoutProgrammeur(Model model) {
        model.addAttribute("nouveauProgrammeur", new Programmeur());
        return "nouveauProgrammeur";
    }

    @PostMapping
    public String creerProgrammeur(@Valid @ModelAttribute("nouveauProgrammeur") Programmeur programmeur,
                                   BindingResult errors, RedirectAttributes redirect) {
        if (errors.hasErrors()) {
            return "nouveauProgrammeur";
        }
        programmeurService.ajouterProgrammeur(programmeur);
        redirect.addFlashAttribute("succes", "Le programmeur a été ajouté.");
        return "redirect:/programmeurs";
    }

    @PutMapping("/{idProgrammeur}")
    public String modifierProgrammeur(@PathVariable Integer idProgrammeur,
                                     @Valid @ModelAttribute("programmeurToUpdate") Programmeur programmeur,
                                     BindingResult errors, RedirectAttributes redirect) {
        programmeurService.getUnProgrammeur(idProgrammeur);
        programmeur.setId(idProgrammeur);
        if (errors.hasErrors()) {
            return "detailsProgrammeur";
        }
        programmeurService.modifierProgrammeur(idProgrammeur, programmeur);
        redirect.addFlashAttribute("succes", "Les modifications ont été enregistrées.");
        return "redirect:/programmeurs";
    }

    @DeleteMapping("/{idProgrammeur}")
    public String deleteProgrammeur(@PathVariable Integer idProgrammeur, RedirectAttributes redirect) {
        programmeurService.supprimerProgrammeur(idProgrammeur);
        redirect.addFlashAttribute("succes", "Le programmeur a été supprimé.");
        return "redirect:/programmeurs";
    }
}
