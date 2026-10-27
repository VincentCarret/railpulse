package altn72.railpulse;

import altn72.railpulse.modele.Incident;
import altn72.railpulse.modele.Prestataire;
import altn72.railpulse.modele.Profil;
import altn72.railpulse.service.IncidentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.util.HtmlUtils;

import java.net.http.HttpClient;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HistoriqueIncidentsIntegrationTests extends SuiviIncidentIntegrationTestSupport {
    @Autowired
    private IncidentService incidentService;

    @Test
    void lesDeuxProfilsVoientUniquementLesResolusTriesParResolution() throws Exception {
        var premierEquipement = creerEquipement("HISTORIQUE-01");
        var secondEquipement = creerEquipement("HISTORIQUE-02");
        Prestataire prestataire = prestataires.save(new Prestataire("Maintenance Réseau", "contact@example.invalid"));
        Incident ancien = new Incident(premierEquipement, "Résolution ancienne", MAINTENANT.minusDays(3));
        ancien.affecter(prestataire);
        ancien.resoudre(MAINTENANT.minusDays(2));
        incidents.save(ancien);
        Incident recent = new Incident(secondEquipement, "Résolution récente", MAINTENANT.minusDays(5));
        recent.resoudre(MAINTENANT.minusHours(1));
        incidents.save(recent);
        Incident memeDate = new Incident(premierEquipement, "Résolution même heure", MAINTENANT.minusDays(1));
        memeDate.resoudre(MAINTENANT.minusHours(1));
        incidents.save(memeDate);
        incidents.save(new Incident(secondEquipement, "Encore non affecté", MAINTENANT.minusHours(2)));
        Incident actifAffecte = new Incident(premierEquipement, "Encore affecté", MAINTENANT.minusHours(3));
        actifAffecte.affecter(prestataire);
        incidents.save(actifAffecte);

        assertEquals(List.of(memeDate.getId(), recent.getId(), ancien.getId()),
                incidentServicePourTest(Profil.AGENT).stream().map(Incident::getId).toList());
        for (Profil profil : Profil.values()) {
            try (HttpClient client = navigateurConnecte(profil)) {
                assertTrue(get(client, "/").body().contains("href=\"/incidents/historique\""));
                var reponse = get(client, "/incidents/historique");
                assertEquals(200, reponse.statusCode());
                String html = HtmlUtils.htmlUnescape(reponse.body());
                assertTrue(html.indexOf("Résolution même heure") < html.indexOf("Résolution récente"));
                assertTrue(html.indexOf("Résolution récente") < html.indexOf("Résolution ancienne"));
                assertFalse(html.contains("Encore non affecté"));
                assertFalse(html.contains("Encore affecté"));
                assertTrue(html.contains("Maintenance Réseau"));
                assertTrue(html.contains("Non affecté"));
                assertTrue(html.contains("HISTORIQUE-01"));
                assertTrue(html.contains("HISTORIQUE-02"));
                assertTrue(html.contains("href=\"/incidents/" + ancien.getId() + "\""));
                assertTrue(html.contains("27/10/2026 11:00"));
            }
        }
    }

    @Test
    void laPageVideEstClaireEtNeModifiePasLesIncidentsActifs() throws Exception {
        var equipement = creerEquipement("HISTORIQUE-VIDE");
        Incident actif = incidents.save(new Incident(equipement, "Toujours actif", MAINTENANT));
        try (HttpClient client = navigateurConnecte(Profil.AGENT)) {
            var reponse = get(client, "/incidents/historique");
            assertEquals(200, reponse.statusCode());
            assertTrue(reponse.body().contains("Aucun incident résolu pour le moment."));
            assertFalse(reponse.body().contains("Toujours actif"));
        }
        assertEquals(1, incidents.count());
        assertNull(incidents.findById(actif.getId()).orElseThrow().getDateResolution());
    }

    @Test
    void laPageExigeUneConnexionEtLeServiceAppliqueLesDroits() throws Exception {
        try (HttpClient client = browser()) {
            assertRedirect(get(client, "/incidents/historique"), "/login");
        }
        SecurityContextHolder.clearContext();
        assertThrows(AuthenticationCredentialsNotFoundException.class, incidentService::consulterHistorique);
        var contexte = SecurityContextHolder.createEmptyContext();
        contexte.setAuthentication(UsernamePasswordAuthenticationToken.authenticated("inconnu", "",
                List.of(new SimpleGrantedAuthority("ROLE_INCONNU"))));
        SecurityContextHolder.setContext(contexte);
        try {
            assertThrows(AccessDeniedException.class, incidentService::consulterHistorique);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void lesDonneesSontEchappeesDansLHistorique() throws Exception {
        var equipement = creerEquipement("HISTORIQUE-XSS");
        Prestataire prestataire = prestataires.save(new Prestataire("<script>prestataire</script>", "contact@example.invalid"));
        Incident incident = new Incident(equipement, "<script>description</script>", MAINTENANT.minusDays(1));
        incident.affecter(prestataire);
        incident.resoudre(MAINTENANT.minusHours(1));
        incidents.save(incident);
        try (HttpClient client = navigateurConnecte(Profil.AGENT)) {
            String html = get(client, "/incidents/historique").body();
            assertFalse(html.contains("<script>"));
            assertTrue(html.contains("&lt;script&gt;description&lt;/script&gt;"));
            assertTrue(html.contains("&lt;script&gt;prestataire&lt;/script&gt;"));
        }
    }

    private List<Incident> incidentServicePourTest(Profil profil) {
        var contexte = SecurityContextHolder.createEmptyContext();
        contexte.setAuthentication(UsernamePasswordAuthenticationToken.authenticated("test", "",
                List.of(new SimpleGrantedAuthority("ROLE_" + profil.name()))));
        SecurityContextHolder.setContext(contexte);
        try {
            return incidentService.consulterHistorique();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
