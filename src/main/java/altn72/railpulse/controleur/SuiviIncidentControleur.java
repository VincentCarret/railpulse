package altn72.railpulse.controleur;

import altn72.railpulse.service.DetailIncident;
import altn72.railpulse.service.IncidentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/incidents")
public class SuiviIncidentControleur {
    private final IncidentService incidentService;

    public SuiviIncidentControleur(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        DetailIncident detail = incidentService.consulter(id);
        model.addAttribute("incident", detail.incident());
        model.addAttribute("evaluation", detail.evaluation());
        model.addAttribute("dureeOuverture", detail.dureeOuverture());
        return "detailIncident";
    }
}
