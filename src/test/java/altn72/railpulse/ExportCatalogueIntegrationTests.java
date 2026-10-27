package altn72.railpulse;

import altn72.railpulse.modele.Equipement;
import altn72.railpulse.modele.EtatFonctionnement;
import altn72.railpulse.modele.Profil;
import altn72.railpulse.modele.Station;
import altn72.railpulse.modele.TypeEquipement;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;

import static org.junit.jupiter.api.Assertions.*;

class ExportCatalogueIntegrationTests extends CatalogueIntegrationTestSupport {
    @Test
    void lesDeuxProfilsExportentLeCatalogueAvecLesTypesEtEtatsEnFrancais() throws Exception {
        creerEquipement("CSV-01");
        for (Profil profil : Profil.values()) {
            var utilisateur = createUser(profil, passwords.encode(PASSWORD));
            try (HttpClient client = browser()) {
                assertRedirect(login(client, utilisateur.getLogin(), PASSWORD), "/");
                assertTrue(get(client, "/stations").body().contains("href=\"/stations/export.csv\""));
                var reponse = get(client, "/stations/export.csv");
                assertEquals(200, reponse.statusCode());
                assertTrue(reponse.headers().firstValue("content-type").orElseThrow().toLowerCase().contains("text/csv"));
                assertTrue(reponse.headers().firstValue("content-disposition").orElseThrow().contains("railpulse-equipements.csv"));
                assertEquals("no-store", reponse.headers().firstValue("cache-control").orElseThrow());
                assertEquals("\uFEFFRéférence;Libellé;Type;État;Station\r\n"
                        + "\"CSV-01\";\"Ascenseur quai A\";\"Ascenseur\";\"En service\";\"République\"\r\n", reponse.body());
            }
        }
        assertEquals(1, equipements.count());
    }

    @Test
    void lesGuillemetsRetoursLigneEtFormulesDeTableurSontProteges() throws Exception {
        Station station = stations.save(new Station("=1+1"));
        equipements.save(new Equipement("CSV-02", "Quai \"A\";\nEntrée", TypeEquipement.PORTIQUE,
                station, EtatFonctionnement.EN_MAINTENANCE));
        try (HttpClient client = authenticatedBrowser()) {
            var reponse = get(client, "/stations/export.csv");
            assertEquals(200, reponse.statusCode());
            assertTrue(reponse.body().contains("\"Quai \"\"A\"\";\nEntrée\""));
            assertTrue(reponse.body().contains("\"'=1+1\""));
        }
    }

    @Test
    void unCatalogueVideProduitUnCsvAvecEnteteEtUnVisiteurDoitSeConnecter() throws Exception {
        try (HttpClient visiteur = browser()) {
            assertRedirect(get(visiteur, "/stations/export.csv"), "/login");
        }
        try (HttpClient client = authenticatedBrowser()) {
            var reponse = get(client, "/stations/export.csv");
            assertEquals(200, reponse.statusCode());
            assertEquals("\uFEFFRéférence;Libellé;Type;État;Station\r\n", reponse.body());
        }
    }
}
