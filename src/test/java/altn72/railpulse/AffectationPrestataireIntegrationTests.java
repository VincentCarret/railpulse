package altn72.railpulse;

import altn72.railpulse.modele.Incident;
import altn72.railpulse.modele.Prestataire;
import altn72.railpulse.modele.Profil;
import altn72.railpulse.modele.StatutIncident;
import altn72.railpulse.service.AffectationPrestataire;
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

class AffectationPrestataireIntegrationTests extends SuiviIncidentIntegrationTestSupport {
    @Autowired
    private IncidentService incidentService;

    @Test
    void leResponsableAffectePuisReaffecteUnIncidentActif() throws Exception {
        var equipement = creerEquipement("AFFECTATION-01");
        Incident incident = incidents.save(new Incident(equipement, "Panne", MAINTENANT.minusHours(4)));
        Prestataire premier = prestataires.save(new Prestataire("Alpha", "alpha@example.invalid"));
        Prestataire second = prestataires.save(new Prestataire("Beta", "beta@example.invalid"));
        String detail = "/incidents/" + incident.getId();
        try (HttpClient client = navigateurConnecte(Profil.RESPONSABLE_EXPLOITATION)) {
            assertTrue(get(client, detail).body().contains("Affecter un prestataire"));
            var formulaire = get(client, detail + "/affecter");
            assertEquals(200, formulaire.statusCode());
            assertTrue(formulaire.body().contains("Alpha"));
            assertTrue(formulaire.body().contains("Beta"));
            assertRedirect(post(client, detail + "/affecter", Map.of("prestataireId", premier.getId().toString(),
                    "_csrf", csrf(formulaire.body()))), detail);
            assertTrue(get(client, detail).body().contains("Réaffecter le prestataire"));
            var reaffectation = get(client, detail + "/affecter");
            assertTrue(reaffectation.body().contains("Prestataire actuel : Alpha"));
            assertRedirect(post(client, detail + "/affecter", Map.of("prestataireId", second.getId().toString(),
                    "_csrf", csrf(reaffectation.body()))), detail);
        }
        Incident conserve = incidents.rechercherDetail(incident.getId()).orElseThrow();
        assertEquals(second.getId(), conserve.getPrestataire().getId());
        assertEquals(StatutIncident.AFFECTE, conserve.getStatut());
        assertEquals("Panne", conserve.getDescription());
        assertEquals(MAINTENANT.minusHours(4), conserve.getDateOuverture());
        assertNull(conserve.getDateResolution());
        assertEquals(1, incidents.count());
    }

    @Test
    void lesSaisiesInvalidesEtLesChampsFalsifiesNeModifientPasLIncident() throws Exception {
        var equipement = creerEquipement("AFFECTATION-02");
        Incident incident = incidents.save(new Incident(equipement, "Panne", MAINTENANT));
        Prestataire prestataire = prestataires.save(new Prestataire("Alpha", "alpha@example.invalid"));
        String action = "/incidents/" + incident.getId() + "/affecter";
        try (HttpClient client = navigateurConnecte(Profil.RESPONSABLE_EXPLOITATION)) {
            String jeton = csrf(get(client, action).body());
            var vide = post(client, action, Map.of("prestataireId", "", "_csrf", jeton));
            assertEquals(200, vide.statusCode());
            assertTrue(vide.body().contains("Choisissez un prestataire"));
            assertEquals(404, post(client, action, Map.of("prestataireId", "9223372036854775807",
                    "_csrf", jeton)).statusCode());
            assertEquals(200, post(client, action, Map.of("prestataireId", "invalide",
                    "_csrf", jeton)).statusCode());
            assertRedirect(post(client, action, Map.of("prestataireId", prestataire.getId().toString(),
                    "statut", "RESOLU", "dateResolution", "2000-01-01T00:00:00",
                    "_csrf", jeton)), "/incidents/" + incident.getId());
        }
        Incident conserve = incidents.rechercherDetail(incident.getId()).orElseThrow();
        assertEquals(prestataire.getId(), conserve.getPrestataire().getId());
        assertEquals(StatutIncident.AFFECTE, conserve.getStatut());
        assertNull(conserve.getDateResolution());
    }

    @Test
    void unIncidentResoluNePeutPasEtreReaffecteMemeAvecUnAncienFormulaire() throws Exception {
        var equipement = creerEquipement("AFFECTATION-03");
        Prestataire premier = prestataires.save(new Prestataire("Alpha", "alpha@example.invalid"));
        Prestataire second = prestataires.save(new Prestataire("Beta", "beta@example.invalid"));
        Incident incident = new Incident(equipement, "Panne", MAINTENANT.minusDays(2));
        incident.affecter(premier);
        incidents.save(incident);
        String detail = "/incidents/" + incident.getId();
        try (HttpClient client = navigateurConnecte(Profil.RESPONSABLE_EXPLOITATION)) {
            String jeton = csrf(get(client, detail + "/affecter").body());
            incident.resoudre(MAINTENANT.minusHours(1));
            incidents.save(incident);
            assertEquals(409, get(client, detail + "/affecter").statusCode());
            assertEquals(409, post(client, detail + "/affecter", Map.of("prestataireId", second.getId().toString(),
                    "_csrf", jeton)).statusCode());
            assertFalse(get(client, detail).body().contains("Réaffecter le prestataire"));
        }
        Incident conserve = incidents.rechercherDetail(incident.getId()).orElseThrow();
        assertEquals(premier.getId(), conserve.getPrestataire().getId());
        assertEquals(StatutIncident.RESOLU, conserve.getStatut());
    }

    @Test
    void lagentEtLesRequetesSansCsrfSontRefuses() throws Exception {
        var equipement = creerEquipement("AFFECTATION-04");
        Incident incident = incidents.save(new Incident(equipement, "Panne", MAINTENANT));
        Prestataire prestataire = prestataires.save(new Prestataire("Alpha", "alpha@example.invalid"));
        String action = "/incidents/" + incident.getId() + "/affecter";
        try (HttpClient client = navigateurConnecte(Profil.AGENT)) {
            assertFalse(get(client, "/incidents/" + incident.getId()).body().contains("Affecter un prestataire"));
            assertEquals(403, get(client, action).statusCode());
            assertEquals(403, post(client, action, Map.of("prestataireId", prestataire.getId().toString(),
                    "_csrf", csrf(get(client, "/").body()))).statusCode());
        }
        try (HttpClient client = navigateurConnecte(Profil.RESPONSABLE_EXPLOITATION)) {
            assertEquals(403, post(client, action, Map.of("prestataireId", prestataire.getId().toString())).statusCode());
        }
        assertNull(incidents.findById(incident.getId()).orElseThrow().getPrestataire());
    }

    @Test
    void leServiceRefuseLesAppelsSansDroit() {
        var equipement = creerEquipement("AFFECTATION-05");
        Incident incident = incidents.save(new Incident(equipement, "Panne", MAINTENANT));
        Prestataire prestataire = prestataires.save(new Prestataire("Alpha", "alpha@example.invalid"));
        AffectationPrestataire demande = new AffectationPrestataire();
        demande.setPrestataireId(prestataire.getId());
        SecurityContextHolder.clearContext();
        assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> incidentService.affecterPrestataire(incident.getId(), demande));
        var contexte = SecurityContextHolder.createEmptyContext();
        contexte.setAuthentication(UsernamePasswordAuthenticationToken.authenticated("agent", "",
                List.of(new SimpleGrantedAuthority("ROLE_AGENT"))));
        SecurityContextHolder.setContext(contexte);
        try {
            assertThrows(AccessDeniedException.class,
                    () -> incidentService.affecterPrestataire(incident.getId(), demande));
        } finally {
            SecurityContextHolder.clearContext();
        }
        assertNull(incidents.findById(incident.getId()).orElseThrow().getPrestataire());
    }
}
