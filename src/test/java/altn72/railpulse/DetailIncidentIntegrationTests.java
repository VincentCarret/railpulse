package altn72.railpulse;

import altn72.railpulse.modele.Evaluation;
import altn72.railpulse.modele.Incident;
import altn72.railpulse.modele.Prestataire;
import altn72.railpulse.modele.Profil;
import altn72.railpulse.service.IncidentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.util.HtmlUtils;

import java.net.http.HttpClient;

import static org.junit.jupiter.api.Assertions.*;

class DetailIncidentIntegrationTests extends SuiviIncidentIntegrationTestSupport {
    @Autowired
    private IncidentService incidentService;

    @Test
    void lesDeuxProfilsConsultentLeDetailDepuisLaFicheAvecLaDureeCourante() throws Exception {
        var equipement = creerEquipement("DETAIL-01");
        Incident incident = incidents.save(new Incident(equipement, "Arrêt intermittent", MAINTENANT.minusHours(47).minusMinutes(30)));
        for (Profil profil : Profil.values()) {
            try (HttpClient client = navigateurConnecte(profil)) {
                String fiche = get(client, "/equipements/" + equipement.getId()).body();
                assertTrue(fiche.contains("href=\"/incidents/" + incident.getId() + "\""));
                var reponse = get(client, "/incidents/" + incident.getId());
                assertEquals(200, reponse.statusCode());
                String html = HtmlUtils.htmlUnescape(reponse.body());
                for (String texte : new String[]{"Arrêt intermittent", "DETAIL-01", "Ascenseur quai A", "République",
                        "1 j 23 h 30 min", "Non affecté", "Aucun prestataire affecté.", "Aucune évaluation enregistrée.",
                        "Incident en cours", "Bonjour Camille (" + profil + ")"}) {
                    assertTrue(html.contains(texte), texte);
                }
                assertTrue(html.contains("href=\"/equipements/" + equipement.getId() + "\""));
                assertTrue(html.contains("action=\"/logout\""));
                csrf(html);
            }
        }
        assertEquals(1, incidents.count());
    }

    @Test
    void unIncidentAffecteAfficheLePrestataireEtSonContact() throws Exception {
        var equipement = creerEquipement("AFFECTE-01");
        Prestataire prestataire = prestataires.save(new Prestataire("Maintenance Réseau", "contact@example.invalid"));
        Incident incident = new Incident(equipement, "Diagnostic à réaliser", MAINTENANT.minusHours(48));
        incident.affecter(prestataire);
        incidents.save(incident);
        try (HttpClient client = navigateurConnecte(Profil.AGENT)) {
            var reponse = get(client, "/incidents/" + incident.getId());
            assertEquals(200, reponse.statusCode());
            assertTrue(reponse.body().contains("Maintenance Réseau"));
            assertTrue(reponse.body().contains("contact@example.invalid"));
            assertTrue(reponse.body().contains("2 j 0 h 0 min"));
            assertTrue(reponse.body().contains("data-statut=\"AFFECTE\""));
        }
    }

    @Test
    void unIncidentResoluAfficheSaDureeFinaleEtSonEvaluation() throws Exception {
        var equipement = creerEquipement("RESOLU-01");
        Prestataire prestataire = prestataires.save(new Prestataire("Maintenance", "contact@example.invalid"));
        Incident incident = new Incident(equipement, "Panne réparée", MAINTENANT.minusDays(10));
        incident.affecter(prestataire);
        incident.resoudre(MAINTENANT.minusDays(8).plusHours(3).plusMinutes(15));
        incidents.save(incident);
        evaluations.save(new Evaluation(incident, 4, "Intervention précise", MAINTENANT.minusDays(7)));
        try (HttpClient client = navigateurConnecte(Profil.RESPONSABLE_EXPLOITATION)) {
            var reponse = get(client, "/incidents/" + incident.getId());
            assertEquals(200, reponse.statusCode());
            String html = HtmlUtils.htmlUnescape(reponse.body());
            assertTrue(html.contains("Durée avant résolution"));
            assertTrue(html.contains("2 j 3 h 15 min"));
            assertTrue(html.contains("19/10/2026 15:15"));
            assertTrue(html.contains("4 / 5"));
            assertTrue(html.contains("Intervention précise"));
            assertTrue(html.contains("20/10/2026 12:00"));
        }
        assertEquals("2 j 3 h 15 min", incidentService.consulter(incident.getId()).dureeOuverture());
    }

    @Test
    void uneEvaluationSansCommentaireEstAfficheeEtResteUniqueParIncident() throws Exception {
        var equipement = creerEquipement("EVALUATION-01");
        Incident incident = new Incident(equipement, "Résolution", MAINTENANT.minusDays(1));
        incident.affecter(prestataires.save(new Prestataire("Maintenance", "contact@example.invalid")));
        incident.resoudre(MAINTENANT.minusHours(12));
        incidents.save(incident);
        evaluations.saveAndFlush(new Evaluation(incident, 5, null, MAINTENANT));
        assertThrows(DataIntegrityViolationException.class,
                () -> evaluations.saveAndFlush(new Evaluation(incident, 3, "Doublon", MAINTENANT)));
        assertEquals(1, evaluations.count());
        try (HttpClient client = navigateurConnecte(Profil.AGENT)) {
            var reponse = get(client, "/incidents/" + incident.getId());
            assertEquals(200, reponse.statusCode());
            assertTrue(reponse.body().contains("Aucun commentaire."));
            assertTrue(reponse.body().contains("5 / 5"));
        }
    }

    @Test
    void lesVisiteursEtLesIdentifiantsInvalidesSontTraitesProprement() throws Exception {
        try (HttpClient visiteur = browser()) {
            assertRedirect(get(visiteur, "/incidents/1"), "/login");
        }
        try (HttpClient client = navigateurConnecte(Profil.AGENT)) {
            var absente = get(client, "/incidents/9223372036854775807");
            assertEquals(404, absente.statusCode());
            assertTrue(HtmlUtils.htmlUnescape(absente.body()).contains("L'incident 9223372036854775807 n'existe pas."));
            assertTrue(absente.body().contains("Bonjour Camille (AGENT)"));
            assertEquals(400, get(client, "/incidents/invalide").statusCode());
            assertEquals(400, get(client, "/incidents/9223372036854775808").statusCode());
        }
    }

    @Test
    void lesDescriptionsEtLesCommentairesSontEchappesSansFuiteDunAutreIncident() throws Exception {
        var equipement = creerEquipement("TEXTE-01");
        Incident incident = new Incident(equipement, "<script>description</script>", MAINTENANT.minusDays(1));
        incident.affecter(prestataires.save(new Prestataire("<script>prestataire</script>", "<b>contact</b>")));
        incident.resoudre(MAINTENANT.minusHours(1));
        incidents.save(incident);
        evaluations.save(new Evaluation(incident, 4, "<script>commentaire</script>", MAINTENANT));
        incidents.save(new Incident(equipement, "Incident à ne pas afficher ici", MAINTENANT));
        try (HttpClient client = navigateurConnecte(Profil.AGENT)) {
            var reponse = get(client, "/incidents/" + incident.getId());
            assertEquals(200, reponse.statusCode());
            assertFalse(reponse.body().contains("<script>"));
            assertTrue(reponse.body().contains("&lt;script&gt;description&lt;/script&gt;"));
            assertTrue(reponse.body().contains("&lt;script&gt;commentaire&lt;/script&gt;"));
            assertFalse(reponse.body().contains("Incident à ne pas afficher ici"));
        }
    }

    @Test
    void uneOuvertureFutureNeProduitPasDeDureeNegative() {
        var equipement = creerEquipement("FUTUR-01");
        Incident incident = incidents.save(new Incident(equipement, "Date importée dans le futur", MAINTENANT.plusMinutes(1)));
        assertEquals("0 j 0 h 0 min", incidentService.consulter(incident.getId()).dureeOuverture());
    }
}
