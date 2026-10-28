package altn72.railpulse.controleur;

import altn72.railpulse.service.FiltrePriseEnCharge;
import altn72.railpulse.service.TableauDeBordService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class TableauDeBordControleur {
    private final TableauDeBordService tableauDeBordService;

    public TableauDeBordControleur(TableauDeBordService tableauDeBordService) {
        this.tableauDeBordService = tableauDeBordService;
    }

    @GetMapping("/tableau-de-bord")
    public String afficher(@RequestParam(required = false) Long stationId,
                           @RequestParam(defaultValue = "TOUS") FiltrePriseEnCharge priseEnCharge,
                           Model model) {
        model.addAttribute("tableau", tableauDeBordService.consulter(stationId, priseEnCharge));
        model.addAttribute("filtresPriseEnCharge", FiltrePriseEnCharge.values());
        return "tableauDeBord";
    }
}
