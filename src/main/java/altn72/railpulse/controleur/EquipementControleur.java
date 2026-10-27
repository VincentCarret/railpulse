package altn72.railpulse.controleur;

import altn72.railpulse.service.EquipementService;
import altn72.railpulse.service.EquipementsStation;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class EquipementControleur {
    private final EquipementService equipementService;

    public EquipementControleur(EquipementService equipementService) {
        this.equipementService = equipementService;
    }

    @GetMapping("/stations")
    public String stations(Model model) {
        model.addAttribute("stations", equipementService.listerStations());
        return "equipements";
    }

    @GetMapping("/stations/{stationId}/equipements")
    public String equipements(@PathVariable Long stationId, Model model) {
        EquipementsStation consultation = equipementService.consulterStation(stationId);
        model.addAttribute("stations", equipementService.listerStations());
        model.addAttribute("stationSelectionnee", consultation.station());
        model.addAttribute("equipements", consultation.equipements());
        return "equipements";
    }
}
