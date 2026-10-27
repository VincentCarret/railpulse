package altn72.railpulse;

import altn72.railpulse.modele.Incident;
import altn72.railpulse.modele.Prestataire;
import altn72.railpulse.modele.Profil;
import altn72.railpulse.modele.StatutIncident;
import altn72.railpulse.service.IncidentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ResolutionIncidentIntegrationTests extends SuiviIncidentIntegrationTestSupport {
    @Autowired
    private IncidentService incidentService;

    @Test
    void leResponsableResoutUnIncidentNonAffecteOuAffecteAvecLaDateDuServeur() throws Exception {
        var equipement = creerEquipement("RESOLUTION-F8-01");
        Prestataire prestataire = prestataires.save(new Prestataire("Maintenance", "contact@example.invalid"));
        try (HttpClient client = navigateurConnecte(Profil.RESPONSABLE_EXPLOITATION)) {
            for (boolean affecte : new boolean[]{false, true}) {
                Incident incident = new Incident(equipement, "Panne", MAINTENANT.minusHours(3));
                if (affecte) {
                    incident.affecter(prestataire);
                }
                incidents.save(incident);
                String detail = "/incidents/" + incident.getId();
                assertTrue(get(client, detail).body().contains("Déclarer l'incident résolu"));
                var confirmation = get(client, detail + "/resoudre");
                assertEquals(200, confirmation.statusCode());
                assertTrue(confirmation.body().contains("Confirmer la résolution"));
                assertRedirect(post(client, detail + "/resoudre", Map.of(
                        "dateResolution", "2000-01-01T00:00:00", "statut", "NON_AFFECTE",
                        "prestataireId", "9223372036854775807", "_csrf", csrf(confirmation.body()))), detail);
                Incident resolu = incidents.rechercherDetail(incident.getId()).orElseThrow();
                assertEquals(StatutIncident.RESOLU, resolu.getStatut());
                assertEquals(MAINTENANT, resolu.getDateResolution());
                assertEquals("Panne", resolu.getDescription());
                assertEquals(MAINTENANT.minusHours(3), resolu.getDateOuverture());
                assertEquals(affecte ? prestataire.getId() : null,
                        resolu.getPrestataire() == null ? null : resolu.getPrestataire().getId());
                String html = get(client, detail).body();
                assertTrue(html.contains("Durée avant résolution"));
                assertFalse(html.contains("Déclarer l'incident résolu"));
                assertFalse(html.contains("Modifier la description"));
                assertFalse(html.contains("Réaffecter le prestataire"));
            }
        }
        assertEquals(2, incidents.count());
    }

    @Test
    void uneSecondeResolutionEstRefuseeEtConserveLaPremiereDate() throws Exception {
        var equipement = creerEquipement("RESOLUTION-F8-02");
        Incident incident = incidents.save(new Incident(equipement, "Panne", MAINTENANT.minusDays(1)));
        String detail = "/incidents/" + incident.getId();
        try (HttpClient client = navigateurConnecte(Profil.RESPONSABLE_EXPLOITATION)) {
            String jeton = csrf(get(client, detail + "/resoudre").body());
            assertRedirect(post(client, detail + "/resoudre", Map.of("_csrf", jeton)), detail);
            assertEquals(409, get(client, detail + "/resoudre").statusCode());
            assertEquals(409, post(client, detail + "/resoudre", Map.of("_csrf", jeton)).statusCode());
        }
        Incident conserve = incidents.findById(incident.getId()).orElseThrow();
        assertEquals(StatutIncident.RESOLU, conserve.getStatut());
        assertEquals(MAINTENANT, conserve.getDateResolution());
    }

    @Test
    void uneOuvertureDansLeFuturNePeutPasEtreResolue() throws Exception {
        var equipement = creerEquipement("RESOLUTION-F8-03");
        Incident incident = incidents.save(new Incident(equipement, "Panne", MAINTENANT.plusHours(1)));
        String action = "/incidents/" + incident.getId() + "/resoudre";
        try (HttpClient client = navigateurConnecte(Profil.RESPONSABLE_EXPLOITATION)) {
            assertEquals(409, post(client, action, Map.of("_csrf", csrf(get(client, action).body()))).statusCode());
        }
        Incident conserve = incidents.findById(incident.getId()).orElseThrow();
        assertEquals(StatutIncident.NON_AFFECTE, conserve.getStatut());
        assertNull(conserve.getDateResolution());
    }

    @Test
    void lagentLeVisiteurEtLesRequetesSansCsrfNePeuventPasResoudre() throws Exception {
        var equipement = creerEquipement("RESOLUTION-F8-04");
        Incident incident = incidents.save(new Incident(equipement, "Panne", MAINTENANT));
        String action = "/incidents/" + incident.getId() + "/resoudre";
        try (HttpClient visiteur = browser()) {
            assertRedirect(get(visiteur, action), "/login");
            assertRedirect(post(visiteur, action, Map.of("_csrf", csrf(get(visiteur, "/login").body()))), "/login");
        }
        try (HttpClient agent = navigateurConnecte(Profil.AGENT)) {
            assertFalse(get(agent, "/incidents/" + incident.getId()).body().contains("Déclarer l'incident résolu"));
            assertEquals(403, get(agent, action).statusCode());
            assertEquals(403, post(agent, action, Map.of("_csrf", csrf(get(agent, "/").body()))).statusCode());
        }
        try (HttpClient responsable = navigateurConnecte(Profil.RESPONSABLE_EXPLOITATION)) {
            assertEquals(403, post(responsable, action, Map.of()).statusCode());
        }
        assertEquals(StatutIncident.NON_AFFECTE, incidents.findById(incident.getId()).orElseThrow().getStatut());
    }

    @Test
    void lesIdentifiantsInvalidesOuAbsentsSontTraitesProprement() throws Exception {
        try (HttpClient client = navigateurConnecte(Profil.RESPONSABLE_EXPLOITATION)) {
            String jeton = csrf(get(client, "/").body());
            assertEquals(404, get(client, "/incidents/9223372036854775807/resoudre").statusCode());
            assertEquals(404, post(client, "/incidents/9223372036854775807/resoudre",
                    Map.of("_csrf", jeton)).statusCode());
            assertEquals(400, get(client, "/incidents/invalide/resoudre").statusCode());
            assertEquals(400, post(client, "/incidents/invalide/resoudre",
                    Map.of("_csrf", jeton)).statusCode());
        }
        assertEquals(0, incidents.count());
    }

    @Test
    void leServiceAppliqueLesDroitsMemeSansPasserParLeControleur() {
        var equipement = creerEquipement("RESOLUTION-F8-05");
        Incident incident = incidents.save(new Incident(equipement, "Panne", MAINTENANT));
        SecurityContextHolder.clearContext();
        assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> incidentService.resoudreIncident(incident.getId()));
        var contexte = SecurityContextHolder.createEmptyContext();
        contexte.setAuthentication(UsernamePasswordAuthenticationToken.authenticated("agent", "",
                List.of(new SimpleGrantedAuthority("ROLE_AGENT"))));
        SecurityContextHolder.setContext(contexte);
        try {
            assertThrows(AccessDeniedException.class, () -> incidentService.preparerResolution(incident.getId()));
            assertThrows(AccessDeniedException.class, () -> incidentService.resoudreIncident(incident.getId()));
        } finally {
            SecurityContextHolder.clearContext();
        }
        assertEquals(StatutIncident.NON_AFFECTE, incidents.findById(incident.getId()).orElseThrow().getStatut());
    }
}
