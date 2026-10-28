package altn72.railpulse;

import altn72.railpulse.modele.Incident;
import altn72.railpulse.modele.Profil;
import altn72.railpulse.modele.StatutIncident;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IncidentApiIntegrationTests extends SuiviIncidentIntegrationTestSupport {
    @Test
    void lesDeuxProfilsConsultentLaListeEtLeDetailEnJson() throws Exception {
        var equipement = creerEquipement("API-F12-01");
        Incident ancien = incidents.save(new Incident(equipement, "Panne ancienne", MAINTENANT.minusDays(2)));
        Incident recent = incidents.save(new Incident(equipement, "Panne récente", MAINTENANT.minusHours(1)));
        for (Profil profil : Profil.values()) {
            try (HttpClient client = navigateurConnecte(profil)) {
                var liste = get(client, "/api/incidents");
                assertEquals(200, liste.statusCode());
                assertTrue(liste.headers().firstValue("content-type").orElse("").contains("application/json"));
                assertTrue(liste.body().indexOf("\"id\":" + recent.getId())
                        < liste.body().indexOf("\"id\":" + ancien.getId()));
                assertTrue(liste.body().contains("\"statut\":\"NON_AFFECTE\""));
                assertTrue(liste.body().contains("\"stationNom\""));
                assertFalse(liste.body().contains("contact@example.invalid"));

                var detail = get(client, "/api/incidents/" + recent.getId());
                assertEquals(200, detail.statusCode());
                assertTrue(detail.body().contains("\"description\":\"Panne récente\""));
                assertTrue(detail.body().contains("\"dateOuverture\":\"2026-10-27T11:00Z\""));
                assertTrue(detail.body().contains("\"dateResolution\":null"));
            }
        }
    }

    @Test
    void leResponsableResoutViaApiEtLaSecondeResolutionEstRefusee() throws Exception {
        var equipement = creerEquipement("API-F12-02");
        Incident incident = incidents.save(new Incident(equipement, "Panne", MAINTENANT.minusHours(2)));
        String chemin = "/api/incidents/" + incident.getId() + "/resolution";
        try (HttpClient client = navigateurConnecte(Profil.RESPONSABLE_EXPLOITATION)) {
            var resolution = resoudre(client, chemin, jetonCsrf(client));
            assertEquals(200, resolution.statusCode());
            assertTrue(resolution.body().contains("\"statut\":\"RESOLU\""));
            assertTrue(resolution.body().contains("\"dateResolution\":\"2026-10-27T12:00Z\""));
            var seconde = resoudre(client, chemin, jetonCsrf(client));
            assertEquals(409, seconde.statusCode());
            assertTrue(seconde.headers().firstValue("content-type").orElse("").contains("application/json"));
            assertTrue(seconde.body().contains("\"code\":409"));
        }
        assertEquals(StatutIncident.RESOLU, incidents.findById(incident.getId()).orElseThrow().getStatut());
        assertEquals(MAINTENANT, incidents.findById(incident.getId()).orElseThrow().getDateResolution());
    }

    @Test
    void visiteurAgentEtRequeteSansCsrfNeModifientRien() throws Exception {
        var equipement = creerEquipement("API-F12-03");
        Incident incident = incidents.save(new Incident(equipement, "Panne", MAINTENANT.minusHours(2)));
        String chemin = "/api/incidents/" + incident.getId() + "/resolution";
        try (HttpClient visiteur = browser()) {
            assertRedirect(get(visiteur, "/api/incidents"), "/login");
            assertRedirect(get(visiteur, "/api/openapi.yaml"), "/login");
            assertRedirect(resoudre(visiteur, chemin, csrf(get(visiteur, "/login").body())), "/login");
        }
        try (HttpClient agent = navigateurConnecte(Profil.AGENT)) {
            assertEquals(403, resoudre(agent, chemin, jetonCsrf(agent)).statusCode());
        }
        try (HttpClient responsable = navigateurConnecte(Profil.RESPONSABLE_EXPLOITATION)) {
            assertEquals(403, responsable.send(HttpRequest.newBuilder(uri(chemin))
                    .timeout(Duration.ofSeconds(10)).POST(HttpRequest.BodyPublishers.noBody()).build(),
                    HttpResponse.BodyHandlers.ofString()).statusCode());
        }
        assertEquals(StatutIncident.NON_AFFECTE, incidents.findById(incident.getId()).orElseThrow().getStatut());
    }

    @Test
    void identifiantsAbsentsOuInvalidesEtTransitionImpossibleOntDesCodesAdaptes() throws Exception {
        var equipement = creerEquipement("API-F12-04");
        Incident futur = incidents.save(new Incident(equipement, "Panne future", MAINTENANT.plusHours(1)));
        try (HttpClient client = navigateurConnecte(Profil.RESPONSABLE_EXPLOITATION)) {
            assertEquals(404, get(client, "/api/incidents/9223372036854775807").statusCode());
            assertEquals(400, get(client, "/api/incidents/invalide").statusCode());
            String jeton = jetonCsrf(client);
            var absent = resoudre(client, "/api/incidents/9223372036854775807/resolution", jeton);
            assertEquals(404, absent.statusCode());
            assertTrue(absent.body().contains("\"code\":404"));
            assertEquals(400, resoudre(client, "/api/incidents/invalide/resolution", jeton).statusCode());
            assertEquals(409, resoudre(client, "/api/incidents/" + futur.getId() + "/resolution", jeton).statusCode());
        }
        assertNull(incidents.findById(futur.getId()).orElseThrow().getDateResolution());
    }

    @Test
    void laSpecificationOpenApiEstDisponiblePourUnUtilisateurConnecte() throws Exception {
        try (HttpClient client = navigateurConnecte(Profil.AGENT)) {
            var reponse = get(client, "/api/openapi.yaml");
            assertEquals(200, reponse.statusCode());
            assertTrue(reponse.body().contains("openapi: 3.0.3"));
            assertTrue(reponse.body().contains("/api/incidents/{id}/resolution:"));
        }
    }

    private String jetonCsrf(HttpClient client) throws Exception {
        var reponse = get(client, "/api/csrf");
        assertEquals(200, reponse.statusCode());
        assertTrue(reponse.body().contains("\"headerName\":\"X-CSRF-TOKEN\""));
        Matcher jeton = Pattern.compile("\"token\":\"([^\"]+)\"").matcher(reponse.body());
        assertTrue(jeton.find());
        return jeton.group(1);
    }

    private HttpResponse<String> resoudre(HttpClient client, String chemin, String jeton) throws Exception {
        return client.send(HttpRequest.newBuilder(uri(chemin)).timeout(Duration.ofSeconds(10))
                .header("X-CSRF-TOKEN", jeton).POST(HttpRequest.BodyPublishers.noBody()).build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
