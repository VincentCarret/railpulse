package altn72.railpulse;

import altn72.railpulse.modele.Equipement;
import altn72.railpulse.modele.EquipementRepository;
import altn72.railpulse.modele.EtatFonctionnement;
import altn72.railpulse.modele.Profil;
import altn72.railpulse.modele.Station;
import altn72.railpulse.modele.StationRepository;
import altn72.railpulse.modele.TypeEquipement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.util.HtmlUtils;

import java.net.http.HttpClient;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EquipementIntegrationTests extends WebIntegrationTestSupport {
    @Autowired
    private StationRepository stations;

    @Autowired
    private EquipementRepository equipements;

    @AfterEach
    void nettoyerCatalogue() {
        equipements.deleteAll();
        stations.deleteAll();
    }

    @Test
    void lesVisiteursDoiventSeConnecterAvantDeConsulterLeCatalogue() throws Exception {
        try (HttpClient client = browser()) {
            assertRedirect(get(client, "/stations"), "/login");
            assertRedirect(get(client, "/stations/1/equipements"), "/login");
        }
    }

    @Test
    void lesDeuxProfilsConsultentUniquementLesEquipementsDeLaStationChoisie() throws Exception {
        Station republique = stations.save(new Station("République"));
        Station centrale = stations.save(new Station("Gare Centrale"));
        equipements.saveAll(List.of(
                new Equipement("REP-02", "Escalier nord", TypeEquipement.ESCALIER_MECANIQUE,
                        republique, EtatFonctionnement.EN_PANNE),
                new Equipement("REP-01", "Ascenseur quai A", TypeEquipement.ASCENSEUR,
                        republique, EtatFonctionnement.EN_SERVICE),
                new Equipement("REP-03", "Portique principal", TypeEquipement.PORTIQUE,
                        republique, EtatFonctionnement.EN_MAINTENANCE),
                new Equipement("GAR-01", "Distributeur hall", TypeEquipement.DISTRIBUTEUR,
                        centrale, EtatFonctionnement.EN_SERVICE)));

        for (Profil profil : Profil.values()) {
            var utilisateur = createUser(profil, passwords.encode(PASSWORD));
            try (HttpClient client = browser()) {
                assertRedirect(login(client, utilisateur.getLogin(), PASSWORD), "/");
                assertTrue(get(client, "/").body().contains("href=\"/stations\""));
                var accueil = get(client, "/stations");
                assertEquals(200, accueil.statusCode());
                assertTrue(accueil.body().indexOf("Gare Centrale") < accueil.body().indexOf("République"));
                assertTrue(accueil.body().contains("Sélectionnez une station"));

                var reponse = get(client, "/stations/" + republique.getId() + "/equipements");
                assertEquals(200, reponse.statusCode());
                String html = HtmlUtils.htmlUnescape(reponse.body());
                for (String texte : List.of("REP-01", "REP-02", "REP-03", "Ascenseur quai A", "Ascenseur",
                        "Escalier mécanique", "Portique", "En service", "En panne", "En maintenance",
                        "Bonjour Camille (" + profil + ")", "aria-current=\"page\"")) {
                    assertTrue(html.contains(texte), texte);
                }
                assertTrue(html.indexOf("REP-01") < html.indexOf("REP-02"));
                assertFalse(html.contains("GAR-01"));
                assertFalse(html.contains("Distributeur hall"));
                assertTrue(html.contains("action=\"/logout\""));
                csrf(html);

                var autreStation = get(client, "/stations/" + centrale.getId() + "/equipements");
                assertEquals(200, autreStation.statusCode());
                assertTrue(autreStation.body().contains("GAR-01"));
                assertFalse(autreStation.body().contains("REP-01"));
            }
        }
        assertEquals(4, equipements.count(), "La consultation ne modifie pas le catalogue.");
    }

    @Test
    void uneStationSansEquipementAfficheUnMessageExplicite() throws Exception {
        Station station = stations.save(new Station("Station vide"));
        try (HttpClient client = authenticatedBrowser()) {
            var reponse = get(client, "/stations/" + station.getId() + "/equipements");
            assertEquals(200, reponse.statusCode());
            assertTrue(reponse.body().contains("Aucun équipement enregistré dans cette station."));
            assertTrue(reponse.body().contains("Station vide"));
            assertTrue(reponse.body().contains("0 équipement(s)"));
        }
    }

    @Test
    void unCatalogueSansStationAfficheUnMessageExplicite() throws Exception {
        try (HttpClient client = authenticatedBrowser()) {
            var reponse = get(client, "/stations");
            assertEquals(200, reponse.statusCode());
            assertTrue(reponse.body().contains("Aucune station enregistrée pour le moment."));
        }
    }

    @Test
    void uneStationAbsenteOuUnIdentifiantInvalideGardentLeBonStatutHttp() throws Exception {
        try (HttpClient client = authenticatedBrowser()) {
            var absente = get(client, "/stations/9223372036854775807/equipements");
            assertEquals(404, absente.statusCode());
            assertTrue(HtmlUtils.htmlUnescape(absente.body()).contains("La station 9223372036854775807 n'existe pas."));
            assertTrue(absente.body().contains("Bonjour Camille (AGENT)"));
            assertEquals(400, get(client, "/stations/pas-un-identifiant/equipements").statusCode());
            assertEquals(400, get(client, "/stations/9223372036854775808/equipements").statusCode());
        }
    }

    @Test
    void lesLibellesDuCatalogueSontEchappesDansLaPage() throws Exception {
        Station station = stations.save(new Station("<script>alert('station')</script>"));
        equipements.save(new Equipement("SEC-01", "<script>alert('equipement')</script>",
                TypeEquipement.ECRAN_INFORMATION, station, EtatFonctionnement.EN_SERVICE));
        try (HttpClient client = authenticatedBrowser()) {
            var reponse = get(client, "/stations/" + station.getId() + "/equipements");
            assertEquals(200, reponse.statusCode());
            assertFalse(reponse.body().contains("<script>"));
            assertTrue(reponse.body().contains("&lt;script&gt;"));
        }
    }

    @Test
    void laReferenceEstUniqueMemeEntreDeuxStationsSelonRg9() {
        Station premiere = stations.save(new Station("Première"));
        Station seconde = stations.save(new Station("Seconde"));
        equipements.saveAndFlush(new Equipement("UNIQUE-01", "Ascenseur", TypeEquipement.ASCENSEUR,
                premiere, EtatFonctionnement.EN_SERVICE));
        assertThrows(DataIntegrityViolationException.class, () -> equipements.saveAndFlush(
                new Equipement("UNIQUE-01", "Autre ascenseur", TypeEquipement.ASCENSEUR,
                        seconde, EtatFonctionnement.EN_PANNE)));
        assertEquals(1, equipements.count());
    }
}
