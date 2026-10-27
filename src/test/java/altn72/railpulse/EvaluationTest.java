package altn72.railpulse;

import altn72.railpulse.modele.Equipement;
import altn72.railpulse.modele.EtatFonctionnement;
import altn72.railpulse.modele.Evaluation;
import altn72.railpulse.modele.Incident;
import altn72.railpulse.modele.Prestataire;
import altn72.railpulse.modele.Station;
import altn72.railpulse.modele.TypeEquipement;
import altn72.railpulse.service.exception.EtatIncidentInvalideException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EvaluationTest {
    private final LocalDateTime ouverture = LocalDateTime.of(2026, 10, 20, 10, 0);

    private Incident incident() {
        return new Incident(new Equipement("TEST-01", "Ascenseur", TypeEquipement.ASCENSEUR,
                new Station("République"), EtatFonctionnement.EN_SERVICE), "Panne", ouverture);
    }

    @Test
    void seulsLesIncidentsResolusAvecPrestatairePeuventEtreEvaluesSelonRg8() {
        Incident incident = incident();
        assertThrows(EtatIncidentInvalideException.class, () -> new Evaluation(null, 4, null, ouverture));
        assertThrows(EtatIncidentInvalideException.class, () -> new Evaluation(incident, 4, null, ouverture));
        incident.resoudre(ouverture);
        assertThrows(EtatIncidentInvalideException.class, () -> new Evaluation(incident, 4, null, ouverture));
        Incident affecte = incident();
        affecte.affecter(new Prestataire("Maintenance", "contact@example.invalid"));
        assertThrows(EtatIncidentInvalideException.class, () -> new Evaluation(affecte, 4, null, ouverture));
    }

    @Test
    void lesNotesSontBorneesDeUnACinqEtLaDateSuitLaResolution() {
        Incident incident = incident();
        incident.affecter(new Prestataire("Maintenance", "contact@example.invalid"));
        incident.resoudre(ouverture.plusHours(1));
        assertThrows(EtatIncidentInvalideException.class, () -> new Evaluation(incident, 0, null, ouverture.plusHours(2)));
        assertThrows(EtatIncidentInvalideException.class, () -> new Evaluation(incident, 6, null, ouverture.plusHours(2)));
        assertThrows(EtatIncidentInvalideException.class, () -> new Evaluation(incident, 3, null, ouverture));
        assertThrows(EtatIncidentInvalideException.class, () -> new Evaluation(incident, 3, null, null));
        assertEquals(1, new Evaluation(incident, 1, null, ouverture.plusHours(1)).getNote());
        assertEquals(5, new Evaluation(incident, 5, null, ouverture.plusHours(2)).getNote());
    }
}
