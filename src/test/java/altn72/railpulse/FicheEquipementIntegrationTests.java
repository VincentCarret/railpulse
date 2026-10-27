package altn72.railpulse;

import altn72.railpulse.modele.Equipement;
import altn72.railpulse.modele.Incident;
import altn72.railpulse.modele.Prestataire;
import altn72.railpulse.modele.Profil;
import org.junit.jupiter.api.Test;
import org.springframework.web.util.HtmlUtils;

import java.net.http.HttpClient;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class FicheEquipementIntegrationTests extends CatalogueIntegrationTestSupport {
    @Test
    void lesDeuxProfilsConsultentLaFicheEtSesIncidentsEnCoursEtPassesSelonRg7() throws Exception {
        Equipement equipement = creerEquipement("FICHE-01");
        Equipement autreEquipement = creerEquipement("AUTRE-01");
        LocalDateTime ouverture = LocalDateTime.of(2026, 10, 20, 10, 30);
        Prestataire prestataire = prestataires.save(new Prestataire("Maintenance Réseau", "contact@example.invalid"));
        Incident resolu = new Incident(equipement, "Ancienne panne réparée", ouverture);
        resolu.affecter(prestataire);
        resolu.resoudre(ouverture.plusDays(1));
        incidents.save(resolu);
        Incident affecte = new Incident(equipement, "Diagnostic en cours", ouverture.plusDays(2));
        affecte.affecter(prestataire);
        incidents.save(affecte);
        incidents.save(new Incident(equipement, "Signalement le plus récent", ouverture.plusDays(3)));
        incidents.save(new Incident(autreEquipement, "Incident étranger à la fiche", ouverture.plusDays(4)));

        for (Profil profil : Profil.values()) {
            var utilisateur = createUser(profil, passwords.encode(PASSWORD));
            try (HttpClient client = browser()) {
                assertRedirect(login(client, utilisateur.getLogin(), PASSWORD), "/");
                String liste = get(client, "/stations/" + equipement.getStation().getId() + "/equipements").body();
                assertTrue(liste.contains("href=\"/equipements/" + equipement.getId() + "\""));
                var reponse = get(client, "/equipements/" + equipement.getId());
                assertEquals(200, reponse.statusCode());
                String html = HtmlUtils.htmlUnescape(reponse.body());
                for (String texte : new String[]{"FICHE-01", "Ascenseur quai A", "République", "Ascenseur", "En service",
                        "Non affecté", "Affecté", "Résolu", "Maintenance Réseau", "20/10/2026 10:30", "21/10/2026 10:30",
                        "Bonjour Camille (" + profil + ")"}) {
                    assertTrue(html.contains(texte), texte);
                }
                assertTrue(html.indexOf("Signalement le plus récent") < html.indexOf("Diagnostic en cours"));
                assertTrue(html.indexOf("Diagnostic en cours") < html.indexOf("Ancienne panne réparée"));
                assertFalse(html.contains("Incident étranger à la fiche"));
                assertTrue(html.contains("action=\"/logout\""));
                csrf(html);
            }
        }
        assertEquals(4, incidents.count());
    }

    @Test
    void uneFicheSansIncidentAfficheUnEtatVideEtLeRetourVersSaStation() throws Exception {
        Equipement equipement = creerEquipement("VIDE-01");
        try (HttpClient client = authenticatedBrowser()) {
            var reponse = get(client, "/equipements/" + equipement.getId());
            assertEquals(200, reponse.statusCode());
            assertTrue(reponse.body().contains("Aucun incident enregistré pour cet équipement."));
            assertTrue(reponse.body().contains("href=\"/stations/" + equipement.getStation().getId() + "/equipements\""));
        }
    }

    @Test
    void laFicheExigeUneConnexion() throws Exception {
        try (HttpClient client = browser()) {
            assertRedirect(get(client, "/equipements/1"), "/login");
        }
    }

    @Test
    void lesEquipementsAbsentsEtLesIdentifiantsInvalidesOntLeBonStatut() throws Exception {
        try (HttpClient client = authenticatedBrowser()) {
            var absente = get(client, "/equipements/9223372036854775807");
            assertEquals(404, absente.statusCode());
            assertTrue(HtmlUtils.htmlUnescape(absente.body()).contains("L'équipement 9223372036854775807 n'existe pas."));
            assertEquals(400, get(client, "/equipements/invalide").statusCode());
        }
    }

    @Test
    void lesDescriptionsEtLesPrestatairesSontAffichesCommeDuTexte() throws Exception {
        Equipement equipement = creerEquipement("XSS-01");
        Prestataire prestataire = prestataires.save(new Prestataire("<script>prestataire</script>", "contact@example.invalid"));
        Incident incident = new Incident(equipement, "<script>description</script>", LocalDateTime.of(2026, 10, 20, 10, 30));
        incident.affecter(prestataire);
        incidents.save(incident);
        try (HttpClient client = authenticatedBrowser()) {
            var reponse = get(client, "/equipements/" + equipement.getId());
            assertEquals(200, reponse.statusCode());
            assertFalse(reponse.body().contains("<script>"));
            assertTrue(reponse.body().contains("&lt;script&gt;description&lt;/script&gt;"));
            assertTrue(reponse.body().contains("&lt;script&gt;prestataire&lt;/script&gt;"));
        }
    }

    @Test
    void deuxIncidentsALaMemeHeureSontTriesParIdentifiantDecroissant() {
        Equipement equipement = creerEquipement("TRI-01");
        LocalDateTime date = LocalDateTime.of(2026, 10, 20, 10, 30);
        Incident premier = incidents.save(new Incident(equipement, "Premier", date));
        Incident second = incidents.save(new Incident(equipement, "Second", date));
        assertEquals(java.util.List.of(second.getId(), premier.getId()), incidents.rechercherParEquipement(equipement.getId())
                .stream().map(Incident::getId).toList());
    }
}
