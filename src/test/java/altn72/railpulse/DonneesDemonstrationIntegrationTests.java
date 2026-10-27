package altn72.railpulse;

import altn72.railpulse.config.DonneesDemonstration;
import altn72.railpulse.modele.Equipement;
import altn72.railpulse.modele.EquipementRepository;
import altn72.railpulse.modele.StationRepository;
import altn72.railpulse.modele.IncidentRepository;
import altn72.railpulse.modele.PrestataireRepository;
import altn72.railpulse.modele.StatutIncident;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("demo")
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:demo-tests;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DonneesDemonstrationIntegrationTests extends WebIntegrationTestSupport {
    @Autowired
    private DonneesDemonstration demonstration;

    @Autowired
    private StationRepository stations;

    @Autowired
    private EquipementRepository equipements;

    @Autowired
    private IncidentRepository incidents;

    @Autowired
    private PrestataireRepository prestataires;

    @Test
    void leProfilDemoInitialiseUnCatalogueVarieSansLeDupliquerAuRedemarrage() {
        assertEquals(3, stations.count());
        assertEquals(5, equipements.count());
        assertEquals(5, incidents.count());
        assertEquals(1, prestataires.count());
        assertEquals(3, incidents.findAll().stream().map(incident -> incident.getStatut()).distinct().count());
        var resolu = incidents.findAll().stream().filter(incident -> incident.getStatut() == StatutIncident.RESOLU).findFirst().orElseThrow();
        assertTrue(resolu.getDateResolution().isAfter(resolu.getDateOuverture()));
        assertEquals(5, equipements.findAll().stream().map(Equipement::getType).distinct().count());
        assertEquals(3, equipements.findAll().stream().map(Equipement::getEtatFonctionnement).distinct().count());
        assertTrue(stations.findAll().stream()
                .anyMatch(station -> equipements.rechercherParStation(station.getId()).isEmpty()));
        var references = equipements.findAll().stream().map(Equipement::getReference).sorted().toList();

        demonstration.run(null);

        assertEquals(3, stations.count());
        assertEquals(5, incidents.count());
        assertEquals(1, prestataires.count());
        assertEquals(references, equipements.findAll().stream().map(Equipement::getReference).sorted().toList());
        assertEquals(0, users.count(), "Le profil demo ne crée aucun compte ni mot de passe.");
    }
}
