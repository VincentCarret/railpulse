package altn72.railpulse;

import altn72.railpulse.config.DonneesDemonstration;
import altn72.railpulse.modele.Equipement;
import altn72.railpulse.modele.EquipementRepository;
import altn72.railpulse.modele.StationRepository;
import altn72.railpulse.modele.IncidentRepository;
import altn72.railpulse.modele.PrestataireRepository;
import altn72.railpulse.modele.StatutIncident;
import altn72.railpulse.modele.EvaluationRepository;
import altn72.railpulse.modele.Profil;
import altn72.railpulse.service.ComptesDemonstrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.net.http.HttpClient;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("demo")
@TestPropertySource(properties = {
        "spring.datasource.url=${TEST_DB_URL:jdbc:h2:mem:demo-tests;MODE=PostgreSQL;DB_CLOSE_DELAY=-1}",
        "spring.jpa.properties.hibernate.default_schema=${TEST_DB_SCHEMA_DEMO:public}"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DonneesDemonstrationIntegrationTests extends WebIntegrationTestSupport {
    private static final String MOT_DE_PASSE_AGENT = UUID.randomUUID().toString();
    private static final String MOT_DE_PASSE_RESPONSABLE = UUID.randomUUID().toString();

    @DynamicPropertySource
    static void secretsDeTest(DynamicPropertyRegistry proprietes) {
        proprietes.add("railpulse.demo.agent-password", () -> MOT_DE_PASSE_AGENT);
        proprietes.add("railpulse.demo.responsable-password", () -> MOT_DE_PASSE_RESPONSABLE);
    }
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

    @Autowired
    private EvaluationRepository evaluations;

    @Test
    void leProfilDemoInitialiseUnCatalogueVarieSansLeDupliquerAuRedemarrage() {
        assertEquals(3, stations.count());
        assertEquals(5, equipements.count());
        assertEquals(5, incidents.count());
        assertEquals(1, prestataires.count());
        assertEquals(3, incidents.findAll().stream().map(incident -> incident.getStatut()).distinct().count());
        var resolu = incidents.findAll().stream().filter(incident -> incident.getStatut() == StatutIncident.RESOLU).findFirst().orElseThrow();
        assertTrue(resolu.getDateResolution().isAfter(resolu.getDateOuverture()));
        assertEquals(4, evaluations.findByIncidentId(resolu.getId()).orElseThrow().getNote());
        assertEquals(5, equipements.findAll().stream().map(Equipement::getType).distinct().count());
        assertEquals(3, equipements.findAll().stream().map(Equipement::getEtatFonctionnement).distinct().count());
        assertTrue(stations.findAll().stream()
                .anyMatch(station -> equipements.rechercherParStation(station.getId()).isEmpty()));
        var references = equipements.findAll().stream().map(Equipement::getReference).sorted().toList();

        demonstration.run(null);

        assertEquals(3, stations.count());
        assertEquals(5, incidents.count());
        assertEquals(1, prestataires.count());
        assertEquals(1, evaluations.count());
        assertEquals(references, equipements.findAll().stream().map(Equipement::getReference).sorted().toList());
        assertEquals(2, users.count());
    }

    @Test
    void lesDeuxComptesDemoSontHachesEtPermettentUneConnexionReelle() throws Exception {
        var agent = users.findByLogin(ComptesDemonstrationService.LOGIN_AGENT).orElseThrow();
        var responsable = users.findByLogin(ComptesDemonstrationService.LOGIN_RESPONSABLE).orElseThrow();
        assertEquals(Profil.AGENT, agent.getProfil());
        assertEquals(Profil.RESPONSABLE_EXPLOITATION, responsable.getProfil());
        assertTrue(agent.getPassword().startsWith("{bcrypt}"));
        assertTrue(responsable.getPassword().startsWith("{bcrypt}"));
        assertTrue(passwords.matches(MOT_DE_PASSE_AGENT, agent.getPassword()));
        assertTrue(passwords.matches(MOT_DE_PASSE_RESPONSABLE, responsable.getPassword()));
        try (HttpClient client = browser()) {
            assertRedirect(login(client, agent.getLogin(), MOT_DE_PASSE_AGENT), "/");
            assertEquals(200, get(client, "/stations").statusCode());
        }
        try (HttpClient client = browser()) {
            assertRedirect(login(client, responsable.getLogin(), MOT_DE_PASSE_RESPONSABLE), "/");
            assertEquals(200, get(client, "/stations").statusCode());
        }
    }
}
