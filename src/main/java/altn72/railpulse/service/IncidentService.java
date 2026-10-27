package altn72.railpulse.service;

import altn72.railpulse.modele.Equipement;
import altn72.railpulse.modele.EvaluationRepository;
import altn72.railpulse.modele.Incident;
import altn72.railpulse.modele.IncidentRepository;
import altn72.railpulse.modele.Prestataire;
import altn72.railpulse.modele.PrestataireRepository;
import altn72.railpulse.modele.StatutIncident;
import altn72.railpulse.service.exception.IncidentIntrouvableException;
import altn72.railpulse.service.exception.PrestataireIntrouvableException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Transactional(readOnly = true)
public class IncidentService {
    private final EquipementService equipementService;
    private final IncidentRepository incidents;
    private final Clock horloge;
    private final EvaluationRepository evaluations;
    private final PrestataireRepository prestataires;

    public IncidentService(EquipementService equipementService, IncidentRepository incidents,
                           Clock horloge, EvaluationRepository evaluations, PrestataireRepository prestataires) {
        this.equipementService = equipementService;
        this.incidents = incidents;
        this.horloge = horloge;
        this.evaluations = evaluations;
        this.prestataires = prestataires;
    }

    public DetailIncident consulter(Long id) {
        Incident incident = lireIncident(id);
        return new DetailIncident(incident, evaluations.findByIncidentId(id).orElse(null), dureeOuverture(incident));
    }

    public Incident lireIncident(Long id) {
        return incidents.rechercherDetail(id).orElseThrow(() -> new IncidentIntrouvableException(id));
    }

    @PreAuthorize("hasAnyRole('AGENT', 'RESPONSABLE_EXPLOITATION')")
    public List<Incident> consulterHistorique() {
        return incidents.rechercherParStatutEtDateResolutionDecroissante(StatutIncident.RESOLU);
    }

    private String dureeOuverture(Incident incident) {
        LocalDateTime fin = incident.getDateResolution() == null ? LocalDateTime.now(horloge) : incident.getDateResolution();
        Duration duree = Duration.between(incident.getDateOuverture(), fin);
        if (duree.isNegative()) {
            duree = Duration.ZERO;
        }
        return "%d j %d h %d min".formatted(duree.toDays(), duree.toHoursPart(), duree.toMinutesPart());
    }

    @Transactional
    public Incident declarer(Long equipementId,
                             @NotNull(message = "La déclaration d'incident est obligatoire.") @Valid DescriptionIncident declaration) {
        Equipement equipement = equipementService.lireEquipement(equipementId);
        Incident incident = new Incident(equipement, declaration.getDescription().strip(), LocalDateTime.now(horloge));
        return incidents.save(incident);
    }

    @PreAuthorize("hasRole('RESPONSABLE_EXPLOITATION')")
    public Incident preparerModification(Long id) {
        Incident incident = lireIncident(id);
        incident.verifierModifiable();
        return incident;
    }

    @Transactional
    @PreAuthorize("hasRole('RESPONSABLE_EXPLOITATION')")
    public void modifierDescription(Long id,
                                    @NotNull(message = "La description de l'incident est obligatoire.") @Valid DescriptionIncident modification) {
        Incident incident = incidents.verrouillerPourModification(id)
                .orElseThrow(() -> new IncidentIntrouvableException(id));
        incident.modifierDescription(modification.getDescription());
    }

    @PreAuthorize("hasRole('RESPONSABLE_EXPLOITATION')")
    public Incident preparerAffectation(Long id) {
        Incident incident = lireIncident(id);
        incident.verifierAffectable();
        return incident;
    }

    @PreAuthorize("hasRole('RESPONSABLE_EXPLOITATION')")
    public List<Prestataire> listerPrestataires() {
        return prestataires.findAllByOrderByNomAscIdAsc();
    }

    @Transactional
    @PreAuthorize("hasRole('RESPONSABLE_EXPLOITATION')")
    public void affecterPrestataire(Long id,
                                   @NotNull(message = "Choisissez un prestataire.") @Valid AffectationPrestataire affectation) {
        Incident incident = incidents.verrouillerPourModification(id)
                .orElseThrow(() -> new IncidentIntrouvableException(id));
        incident.verifierAffectable();
        Prestataire prestataire = prestataires.findById(affectation.getPrestataireId())
                .orElseThrow(() -> new PrestataireIntrouvableException(affectation.getPrestataireId()));
        incident.affecter(prestataire);
    }

    @PreAuthorize("hasRole('RESPONSABLE_EXPLOITATION')")
    public Incident preparerResolution(Long id) {
        Incident incident = lireIncident(id);
        incident.verifierResolvable();
        return incident;
    }

    @Transactional
    @PreAuthorize("hasRole('RESPONSABLE_EXPLOITATION')")
    public void resoudreIncident(Long id) {
        Incident incident = incidents.verrouillerPourModification(id)
                .orElseThrow(() -> new IncidentIntrouvableException(id));
        incident.resoudre(LocalDateTime.now(horloge));
    }
}
