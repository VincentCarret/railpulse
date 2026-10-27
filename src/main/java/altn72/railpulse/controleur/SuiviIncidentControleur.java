package altn72.railpulse.controleur;

import altn72.railpulse.modele.Incident;
import altn72.railpulse.service.DescriptionIncident;
import altn72.railpulse.service.DetailIncident;
import altn72.railpulse.service.DroitsIncident;
import altn72.railpulse.service.IncidentService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
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
@RequestMapping("/incidents")
public class SuiviIncidentControleur {
    private final IncidentService incidentService;
    private final DroitsIncident droitsIncident;

    public SuiviIncidentControleur(IncidentService incidentService, DroitsIncident droitsIncident) {
        this.incidentService = incidentService;
        this.droitsIncident = droitsIncident;
    }

    @InitBinder("modificationIncident")
    public void limiterChamps(WebDataBinder binder) {
        binder.setAllowedFields("description");
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model, Authentication authentication) {
        DetailIncident detail = incidentService.consulter(id);
        model.addAttribute("incident", detail.incident());
        model.addAttribute("evaluation", detail.evaluation());
        model.addAttribute("dureeOuverture", detail.dureeOuverture());
        model.addAttribute("modificationAutorisee", droitsIncident.peutModifier(detail.incident(), authentication));
        return "detailIncident";
    }

    @GetMapping("/{id}/modifier")
    public String preparerModification(@PathVariable Long id, Model model) {
        Incident incident = incidentService.preparerModification(id);
        DescriptionIncident modification = new DescriptionIncident();
        modification.setDescription(incident.getDescription());
        model.addAttribute("modificationIncident", modification);
        return formulaire(incident, model);
    }

    @PostMapping("/{id}")
    public String modifier(@PathVariable Long id,
                           @Valid @ModelAttribute("modificationIncident") DescriptionIncident modification,
                           BindingResult erreurs, Model model, RedirectAttributes redirection) {
        if (erreurs.hasErrors()) {
            return formulaire(incidentService.preparerModification(id), model);
        }
        incidentService.modifierDescription(id, modification);
        redirection.addFlashAttribute("succes", "La description de l'incident a été mise à jour.");
        return "redirect:/incidents/" + id;
    }

    private String formulaire(Incident incident, Model model) {
        model.addAttribute("incident", incident);
        model.addAttribute("longueurDescriptionMax", Incident.LONGUEUR_DESCRIPTION_MAX);
        return "modifierIncident";
    }
}
