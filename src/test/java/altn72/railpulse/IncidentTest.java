package altn72.railpulse;

import altn72.railpulse.modele.Incident;
import altn72.railpulse.modele.Prestataire;
import altn72.railpulse.modele.StatutIncident;
import altn72.railpulse.modele.Equipement;
import altn72.railpulse.modele.Station;
import altn72.railpulse.modele.TypeEquipement;
import altn72.railpulse.modele.EtatFonctionnement;
import altn72.railpulse.service.exception.EtatIncidentInvalideException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class IncidentTest {
    private final LocalDateTime ouverture = LocalDateTime.of(2026, 10, 20, 10, 30);
    private final Prestataire prestataire = new Prestataire("Maintenance", "contact@example.invalid");
    private final Equipement equipement = new Equipement("TEST-01", "Ascenseur", TypeEquipement.ASCENSEUR,
            new Station("République"), EtatFonctionnement.EN_SERVICE);

    @Test
    void unIncidentDemarreNonAffecteSansPrestataireNiResolution() {
        Incident incident = new Incident(equipement, "Signalement", ouverture);
        assertEquals(StatutIncident.NON_AFFECTE, incident.getStatut());
        assertNull(incident.getPrestataire());
        assertNull(incident.getDateResolution());
    }

    @Test
    void uneAffectationSansPrestataireNeChangePasLEtat() {
        Incident incident = new Incident(equipement, "Signalement", ouverture);
        assertThrows(EtatIncidentInvalideException.class, () -> incident.affecter(null));
        assertEquals(StatutIncident.NON_AFFECTE, incident.getStatut());
    }

    @Test
    void uneResolutionAnterieureOuAbsenteEstRefuseeSansMutation() {
        Incident incident = new Incident(equipement, "Signalement", ouverture);
        incident.affecter(prestataire);
        assertThrows(EtatIncidentInvalideException.class, () -> incident.resoudre(ouverture.minusSeconds(1)));
        assertThrows(EtatIncidentInvalideException.class, () -> incident.resoudre(null));
        assertEquals(StatutIncident.AFFECTE, incident.getStatut());
        assertNull(incident.getDateResolution());
    }

    @Test
    void laResolutionPeutEgalerLOuvertureEtInterditLesAffectationsSuivantes() {
        Incident incident = new Incident(equipement, "Signalement", ouverture);
        incident.affecter(prestataire);
        incident.resoudre(ouverture);
        assertThrows(EtatIncidentInvalideException.class, () -> incident.affecter(prestataire));
        assertThrows(EtatIncidentInvalideException.class, () -> incident.resoudre(ouverture.plusDays(1)));
        assertEquals(StatutIncident.RESOLU, incident.getStatut());
        assertEquals(ouverture, incident.getDateResolution());
        assertSame(prestataire, incident.getPrestataire());
    }
}
