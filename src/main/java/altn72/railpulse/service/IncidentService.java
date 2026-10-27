package altn72.railpulse.service;

import altn72.railpulse.modele.Equipement;
import altn72.railpulse.modele.Incident;
import altn72.railpulse.modele.IncidentRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@Validated
public class IncidentService {
    private final EquipementService equipementService;
    private final IncidentRepository incidents;
    private final Clock horloge;

    public IncidentService(EquipementService equipementService, IncidentRepository incidents, Clock horloge) {
        this.equipementService = equipementService;
        this.incidents = incidents;
        this.horloge = horloge;
    }

    @Transactional
    public Incident declarer(Long equipementId,
                             @NotNull(message = "La déclaration d'incident est obligatoire.") @Valid DeclarationIncident declaration) {
        Equipement equipement = equipementService.lireEquipement(equipementId);
        Incident incident = new Incident(equipement, declaration.getDescription().strip(), LocalDateTime.now(horloge));
        return incidents.save(incident);
    }
}
