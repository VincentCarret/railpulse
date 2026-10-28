package altn72.railpulse;

import altn72.railpulse.modele.Evaluation;
import altn72.railpulse.modele.Equipement;
import altn72.railpulse.modele.Incident;
import altn72.railpulse.modele.Prestataire;
import altn72.railpulse.modele.Profil;
import altn72.railpulse.service.EvaluationService;
import altn72.railpulse.service.NouvelleEvaluation;
import jakarta.validation.ConstraintViolationException;
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

class SaisieEvaluationIntegrationTests extends SuiviIncidentIntegrationTestSupport {
    @Autowired
    private EvaluationService evaluationService;

    @Test
    void leResponsableEvalueUneInterventionEtLesDeuxProfilsConsultentLaNote() throws Exception {
        var equipement = creerEquipement("EVALUATION-F10-01");
        Prestataire prestataire = prestataires.save(new Prestataire("Maintenance", "contact@example.invalid"));
        Incident incident = incidentResolu(equipement, prestataire);
        String detail = "/incidents/" + incident.getId();
        try (HttpClient client = navigateurConnecte(Profil.RESPONSABLE_EXPLOITATION)) {
            assertTrue(get(client, detail).body().contains("Évaluer l'intervention"));
            var formulaire = get(client, detail + "/evaluer");
            assertEquals(200, formulaire.statusCode());
            assertTrue(formulaire.body().contains("Maintenance"));
            assertRedirect(post(client, detail + "/evaluer", Map.of("note", "5",
                    "commentaire", "  Intervention efficace  ", "_csrf", csrf(formulaire.body()))), detail);
            String html = get(client, detail).body();
            assertTrue(html.contains("5 / 5"));
            assertTrue(html.contains("Intervention efficace"));
            assertFalse(html.contains("Évaluer l'intervention"));
        }
        Evaluation conservee = evaluations.findByIncidentId(incident.getId()).orElseThrow();
        assertEquals(5, conservee.getNote());
        assertEquals("Intervention efficace", conservee.getCommentaire());
        assertEquals(MAINTENANT, conservee.getDate());
        assertEquals(1, evaluations.count());
        try (HttpClient agent = navigateurConnecte(Profil.AGENT)) {
            String html = get(agent, detail).body();
            assertTrue(html.contains("5 / 5"));
            assertTrue(html.contains("Intervention efficace"));
            assertFalse(html.contains("Évaluer l'intervention"));
        }
    }

    @Test
    void laNoteMinimaleEtLeCommentaireFacultatifSontAcceptesUneSeuleFois() throws Exception {
        var equipement = creerEquipement("EVALUATION-F10-02");
        Prestataire prestataire = prestataires.save(new Prestataire("Maintenance", "contact@example.invalid"));
        Incident incident = incidentResolu(equipement, prestataire);
        String action = "/incidents/" + incident.getId() + "/evaluer";
        try (HttpClient client = navigateurConnecte(Profil.RESPONSABLE_EXPLOITATION)) {
            String jeton = csrf(get(client, action).body());
            assertRedirect(post(client, action, Map.of("note", "1", "_csrf", jeton)),
                    "/incidents/" + incident.getId());
            assertEquals(409, get(client, action).statusCode());
            assertEquals(409, post(client, action, Map.of("note", "4", "_csrf", jeton)).statusCode());
        }
        Evaluation conservee = evaluations.findByIncidentId(incident.getId()).orElseThrow();
        assertEquals(1, conservee.getNote());
        assertNull(conservee.getCommentaire());
        assertEquals(1, evaluations.count());
    }

    @Test
    void incidentActifOuResoluSansPrestataireNePeutPasEtreEvalue() throws Exception {
        var equipement = creerEquipement("EVALUATION-F10-03");
        Prestataire prestataire = prestataires.save(new Prestataire("Maintenance", "contact@example.invalid"));
        Incident actif = new Incident(equipement, "Panne", MAINTENANT.minusDays(1));
        actif.affecter(prestataire);
        incidents.save(actif);
        Incident sansPrestataire = incidents.save(new Incident(equipement, "Autre panne", MAINTENANT.minusDays(1)));
        sansPrestataire.resoudre(MAINTENANT.minusHours(2));
        incidents.save(sansPrestataire);
        try (HttpClient client = navigateurConnecte(Profil.RESPONSABLE_EXPLOITATION)) {
            String jeton = csrf(get(client, "/").body());
            for (Incident incident : List.of(actif, sansPrestataire)) {
                String detail = "/incidents/" + incident.getId();
                assertFalse(get(client, detail).body().contains("Évaluer l'intervention"));
                assertEquals(409, get(client, detail + "/evaluer").statusCode());
                assertEquals(409, post(client, detail + "/evaluer", Map.of("note", "4", "_csrf", jeton)).statusCode());
            }
        }
        assertEquals(0, evaluations.count());
    }

    @Test
    void notesEtCommentairesInvalidesNeCreentAucuneEvaluation() throws Exception {
        var equipement = creerEquipement("EVALUATION-F10-04");
        Prestataire prestataire = prestataires.save(new Prestataire("Maintenance", "contact@example.invalid"));
        Incident incident = incidentResolu(equipement, prestataire);
        String action = "/incidents/" + incident.getId() + "/evaluer";
        try (HttpClient client = navigateurConnecte(Profil.RESPONSABLE_EXPLOITATION)) {
            String jeton = csrf(get(client, action).body());
            for (String note : List.of("", "0", "6", "incorrect")) {
                var reponse = post(client, action, Map.of("note", note, "_csrf", jeton));
                assertEquals(200, reponse.statusCode());
                assertTrue(reponse.body().contains("Aucune évaluation enregistrée"));
            }
            var tropLong = post(client, action, Map.of("note", "3", "commentaire", "x".repeat(2001), "_csrf", jeton));
            assertEquals(200, tropLong.statusCode());
            assertTrue(tropLong.body().contains("2000 caractères"));
        }
        assertEquals(0, evaluations.count());
    }

    @Test
    void champsFalsifiesEtHtmlDuCommentaireNeSontPasInterpretes() throws Exception {
        var equipement = creerEquipement("EVALUATION-F10-05");
        Prestataire prestataire = prestataires.save(new Prestataire("Maintenance", "contact@example.invalid"));
        Incident incident = incidentResolu(equipement, prestataire);
        String detail = "/incidents/" + incident.getId();
        try (HttpClient client = navigateurConnecte(Profil.RESPONSABLE_EXPLOITATION)) {
            String jeton = csrf(get(client, detail + "/evaluer").body());
            assertRedirect(post(client, detail + "/evaluer", Map.of("note", "3",
                    "commentaire", "<script>avis</script>", "incidentId", "9223372036854775807",
                    "date", "2000-01-01T00:00:00", "_csrf", jeton)), detail);
            String html = get(client, detail).body();
            assertFalse(html.contains("<script>"));
            assertTrue(html.contains("&lt;script&gt;avis&lt;/script&gt;"));
        }
        Evaluation conservee = evaluations.findByIncidentId(incident.getId()).orElseThrow();
        assertEquals(incident.getId(), conservee.getIncident().getId());
        assertEquals(MAINTENANT, conservee.getDate());
    }

    @Test
    void droitsCsrfEtIdentifiantsInvalidesSontRefuses() throws Exception {
        var equipement = creerEquipement("EVALUATION-F10-06");
        Prestataire prestataire = prestataires.save(new Prestataire("Maintenance", "contact@example.invalid"));
        Incident incident = incidentResolu(equipement, prestataire);
        String action = "/incidents/" + incident.getId() + "/evaluer";
        try (HttpClient visiteur = browser()) {
            assertRedirect(get(visiteur, action), "/login");
            assertRedirect(post(visiteur, action, Map.of("note", "4", "_csrf", csrf(get(visiteur, "/login").body()))), "/login");
        }
        try (HttpClient agent = navigateurConnecte(Profil.AGENT)) {
            assertEquals(403, get(agent, action).statusCode());
            assertEquals(403, post(agent, action, Map.of("note", "4", "_csrf", csrf(get(agent, "/").body()))).statusCode());
        }
        try (HttpClient responsable = navigateurConnecte(Profil.RESPONSABLE_EXPLOITATION)) {
            assertEquals(403, post(responsable, action, Map.of("note", "4")).statusCode());
            String jeton = csrf(get(responsable, "/").body());
            assertEquals(404, get(responsable, "/incidents/9223372036854775807/evaluer").statusCode());
            assertEquals(404, post(responsable, "/incidents/9223372036854775807/evaluer",
                    Map.of("note", "4", "_csrf", jeton)).statusCode());
            assertEquals(400, get(responsable, "/incidents/invalide/evaluer").statusCode());
            assertEquals(400, post(responsable, "/incidents/invalide/evaluer",
                    Map.of("note", "4", "_csrf", jeton)).statusCode());
        }
        assertEquals(0, evaluations.count());
    }

    @Test
    void leServiceAppliqueDroitsEtValidationSansControleur() {
        var equipement = creerEquipement("EVALUATION-F10-07");
        Prestataire prestataire = prestataires.save(new Prestataire("Maintenance", "contact@example.invalid"));
        Incident incident = incidentResolu(equipement, prestataire);
        NouvelleEvaluation saisie = new NouvelleEvaluation();
        saisie.setNote(4);
        SecurityContextHolder.clearContext();
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> evaluationService.evaluer(incident.getId(), saisie));
        var contexte = SecurityContextHolder.createEmptyContext();
        contexte.setAuthentication(UsernamePasswordAuthenticationToken.authenticated("agent", "",
                List.of(new SimpleGrantedAuthority("ROLE_AGENT"))));
        SecurityContextHolder.setContext(contexte);
        try {
            assertThrows(AccessDeniedException.class, () -> evaluationService.preparerEvaluation(incident.getId()));
            assertThrows(AccessDeniedException.class, () -> evaluationService.evaluer(incident.getId(), saisie));
        } finally {
            SecurityContextHolder.clearContext();
        }
        contexte = SecurityContextHolder.createEmptyContext();
        contexte.setAuthentication(UsernamePasswordAuthenticationToken.authenticated("responsable", "",
                List.of(new SimpleGrantedAuthority("ROLE_RESPONSABLE_EXPLOITATION"))));
        SecurityContextHolder.setContext(contexte);
        try {
            saisie.setNote(0);
            assertThrows(ConstraintViolationException.class, () -> evaluationService.evaluer(incident.getId(), saisie));
        } finally {
            SecurityContextHolder.clearContext();
        }
        assertEquals(0, evaluations.count());
    }

    private Incident incidentResolu(Equipement equipement, Prestataire prestataire) {
        Incident incident = new Incident(equipement, "Panne réparée", MAINTENANT.minusDays(2));
        incident.affecter(prestataire);
        incident.resoudre(MAINTENANT.minusHours(1));
        return incidents.save(incident);
    }
}
