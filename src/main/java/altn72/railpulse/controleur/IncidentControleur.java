package altn72.railpulse.controleur;

import altn72.railpulse.modele.Incident;
import altn72.railpulse.service.DescriptionIncident;
import altn72.railpulse.service.EquipementService;
import altn72.railpulse.service.IncidentService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/equipements/{equipementId}/incidents")
public class IncidentControleur {
    private final EquipementService equipementService;
    private final IncidentService incidentService;

    public IncidentControleur(EquipementService equipementService, IncidentService incidentService) {
        this.equipementService = equipementService;
        this.incidentService = incidentService;
    }

    @InitBinder("declarationIncident")
    public void limiterChamps(WebDataBinder binder) {
        binder.setAllowedFields("description");
    }

    @GetMapping("/nouveau")
    public String nouveau(@PathVariable Long equipementId, Model model) {
        model.addAttribute("declarationIncident", new DescriptionIncident());
        return formulaire(equipementId, model);
    }

    @PostMapping
    public String declarer(@PathVariable Long equipementId,
                           @Valid @ModelAttribute("declarationIncident") DescriptionIncident declaration,
                           BindingResult erreurs, Model model, RedirectAttributes redirection) {
        if (erreurs.hasErrors()) {
            return formulaire(equipementId, model);
        }
        incidentService.declarer(equipementId, declaration);
        redirection.addFlashAttribute("succes", "L'incident a été déclaré. Il est en attente d'affectation.");
        return "redirect:/equipements/" + equipementId;
    }

    private String formulaire(Long equipementId, Model model) {
        model.addAttribute("equipement", equipementService.lireEquipement(equipementId));
        model.addAttribute("longueurDescriptionMax", Incident.LONGUEUR_DESCRIPTION_MAX);
        return "nouvelIncident";
    }
}
