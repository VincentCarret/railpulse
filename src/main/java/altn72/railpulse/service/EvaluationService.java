package altn72.railpulse.service;

import altn72.railpulse.modele.Evaluation;
import altn72.railpulse.modele.EvaluationRepository;
import altn72.railpulse.modele.Incident;
import altn72.railpulse.modele.IncidentRepository;
import altn72.railpulse.service.exception.EtatIncidentInvalideException;
import altn72.railpulse.service.exception.IncidentIntrouvableException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Transactional(readOnly = true)
public class EvaluationService {
    private final IncidentRepository incidents;
    private final EvaluationRepository evaluations;
    private final Clock horloge;

    public EvaluationService(IncidentRepository incidents, EvaluationRepository evaluations, Clock horloge) {
        this.incidents = incidents;
        this.evaluations = evaluations;
        this.horloge = horloge;
    }

    @PreAuthorize("hasRole('RESPONSABLE_EXPLOITATION')")
    public Incident preparerEvaluation(Long incidentId) {
        Incident incident = incidents.rechercherDetail(incidentId)
                .orElseThrow(() -> new IncidentIntrouvableException(incidentId));
        verifierEligible(incident);
        return incident;
    }

    @Transactional
    @PreAuthorize("hasRole('RESPONSABLE_EXPLOITATION')")
    public void evaluer(Long incidentId,
                        @NotNull(message = "L'évaluation est obligatoire.") @Valid NouvelleEvaluation saisie) {
        Incident incident = incidents.verrouillerPourModification(incidentId)
                .orElseThrow(() -> new IncidentIntrouvableException(incidentId));
        verifierEligible(incident);
        evaluations.save(new Evaluation(incident, saisie.getNote(), saisie.getCommentaire(), LocalDateTime.now(horloge)));
    }

    private void verifierEligible(Incident incident) {
        incident.verifierEvaluable();
        if (evaluations.existsByIncidentId(incident.getId())) {
            throw new EtatIncidentInvalideException("Cet incident possède déjà une évaluation.");
        }
    }
}
