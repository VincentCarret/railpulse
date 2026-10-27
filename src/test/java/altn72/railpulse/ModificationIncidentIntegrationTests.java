package altn72.railpulse;

import altn72.railpulse.modele.Incident;
import altn72.railpulse.modele.Prestataire;
import altn72.railpulse.modele.Profil;
import altn72.railpulse.modele.StatutIncident;
import altn72.railpulse.service.DescriptionIncident;
import altn72.railpulse.service.IncidentService;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.util.HtmlUtils;

import java.net.http.HttpClient;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ModificationIncidentIntegrationTests extends SuiviIncidentIntegrationTestSupport {
    @Autowired
    private IncidentService incidentService;

    @Test
    void leResponsableCorrigeLaDescriptionDesIncidentsNonAffectesEtAffectes() throws Exception {
        var equipement = creerEquipement("MODIFICATION-01");
        Prestataire prestataire = prestataires.save(new Prestataire("Maintenance", "contact@example.invalid"));
        try (HttpClient client = navigateurConnecte(Profil.RESPONSABLE_EXPLOITATION)) {
            for (boolean affecte : new boolean[]{false, true}) {
                Incident incident = new Incident(equipement, "Description initiale", MAINTENANT.minusDays(1));
                if (affecte) {
                    incident.affecter(prestataire);
                }
                incidents.save(incident);
                String detail = "/incidents/" + incident.getId();
                assertTrue(get(client, detail).body().contains("href=\"" + detail + "/modifier\""));
                var formulaire = get(client, detail + "/modifier");
                assertEquals(200, formulaire.statusCode());
                assertTrue(formulaire.body().contains("Description initiale"));
                assertTrue(formulaire.body().contains("MODIFICATION-01"));
                assertRedirect(post(client, detail, Map.of("description", "  Description corrigée  ",
                        "_csrf", csrf(formulaire.body()))), detail);
                Incident modifie = incidents.rechercherDetail(incident.getId()).orElseThrow();
                assertEquals("Description corrigée", modifie.getDescription());
                assertEquals(incident.getStatut(), modifie.getStatut());
                assertEquals(incident.getDateOuverture(), modifie.getDateOuverture());
                assertEquals(equipement.getId(), modifie.getEquipement().getId());
                assertNull(modifie.getDateResolution());
                if (affecte) {
                    assertEquals(prestataire.getId(), modifie.getPrestataire().getId());
                } else {
                    assertNull(modifie.getPrestataire());
                }
                assertTrue(HtmlUtils.htmlUnescape(get(client, detail).body()).contains("La description de l'incident a été mise à jour."));
                assertFalse(HtmlUtils.htmlUnescape(get(client, detail).body()).contains("La description de l'incident a été mise à jour."));
            }
        }
        assertEquals(2, incidents.count());
    }

    @Test
    void lagentPeutLireMaisNePeutPasOuvrirLeFormulaireNiEnvoyerUneModification() throws Exception {
        var equipement = creerEquipement("DROITS-01");
        Incident incident = incidents.save(new Incident(equipement, "À conserver", MAINTENANT.minusHours(1)));
        String detail = "/incidents/" + incident.getId();
        try (HttpClient client = navigateurConnecte(Profil.AGENT)) {
            var lecture = get(client, detail);
            assertEquals(200, lecture.statusCode());
            assertFalse(lecture.body().contains("Modifier la description"));
            assertEquals(403, get(client, detail + "/modifier").statusCode());
            assertEquals(403, post(client, detail, Map.of("description", "Interdit", "_csrf", csrf(lecture.body()))).statusCode());
        }
        assertEquals("À conserver", incidents.findById(incident.getId()).orElseThrow().getDescription());
    }

    @Test
    void laValidationConserveLaSaisieEtLesDonneesExistantes() throws Exception {
        var equipement = creerEquipement("VALIDATION-01");
        Incident incident = incidents.save(new Incident(equipement, "Description conservée en base", MAINTENANT));
        String detail = "/incidents/" + incident.getId();
        try (HttpClient client = navigateurConnecte(Profil.RESPONSABLE_EXPLOITATION)) {
            for (Map<String, String> saisie : List.of(Map.<String, String>of(), Map.of("description", ""),
                    Map.of("description", " \n\t "), Map.of("description", "Saisie conservée " + "x".repeat(2000)))) {
                Map<String, String> champs = new HashMap<>(saisie);
                champs.put("_csrf", csrf(get(client, detail + "/modifier").body()));
                var reponse = post(client, detail, champs);
                assertEquals(200, reponse.statusCode());
                assertTrue(reponse.body().contains("Aucune modification n’a été enregistrée."));
                assertTrue(reponse.body().contains("aria-invalid=\"true\""));
                assertEquals("Description conservée en base", incidents.findById(incident.getId()).orElseThrow().getDescription());
                if (saisie.getOrDefault("description", "").startsWith("Saisie conservée")) {
                    assertTrue(HtmlUtils.htmlUnescape(reponse.body()).contains(saisie.get("description")));
                    assertTrue(reponse.body().contains("La description ne doit pas dépasser 2000 caractères."));
                }
            }
        }
        assertEquals(1, incidents.count());
    }

    @Test
    void unIncidentResoluEntreLouvertureDuFormulaireEtLenvoiNePeutPlusEtreModifie() throws Exception {
        var equipement = creerEquipement("RESOLUTION-01");
        Incident incident = incidents.save(new Incident(equipement, "Historique à conserver", MAINTENANT.minusDays(1)));
        String detail = "/incidents/" + incident.getId();
        try (HttpClient client = navigateurConnecte(Profil.RESPONSABLE_EXPLOITATION)) {
            String jeton = csrf(get(client, detail + "/modifier").body());
            incident.resoudre(MAINTENANT.minusHours(1));
            incidents.save(incident);
            assertEquals(409, post(client, detail, Map.of("description", "Trop tard", "_csrf", jeton)).statusCode());
            assertEquals(409, post(client, detail, Map.of("description", "", "_csrf", jeton)).statusCode());
            var formulaireFerme = get(client, detail + "/modifier");
            assertEquals(409, formulaireFerme.statusCode());
            assertTrue(HtmlUtils.htmlUnescape(formulaireFerme.body()).contains("La description d'un incident résolu ne peut plus être modifiée."));
            assertFalse(get(client, detail).body().contains("Modifier la description"));
        }
        Incident conserve = incidents.findById(incident.getId()).orElseThrow();
        assertEquals("Historique à conserver", conserve.getDescription());
        assertEquals(StatutIncident.RESOLU, conserve.getStatut());
        assertEquals(MAINTENANT.minusHours(1), conserve.getDateResolution());
    }

    @Test
    void lesChampsFalsifiesNeChangentNiLaCibleNiLesDatesNiLAffectation() throws Exception {
        var equipement = creerEquipement("CIBLE-01");
        var autreEquipement = creerEquipement("AUTRE-01");
        Prestataire prestataire = prestataires.save(new Prestataire("Maintenance", "contact@example.invalid"));
        Incident incident = new Incident(equipement, "À corriger", MAINTENANT.minusDays(1));
        incident.affecter(prestataire);
        incidents.save(incident);
        Incident autre = incidents.save(new Incident(autreEquipement, "Ne pas toucher", MAINTENANT));
        String detail = "/incidents/" + incident.getId();
        try (HttpClient client = navigateurConnecte(Profil.RESPONSABLE_EXPLOITATION)) {
            Map<String, String> champs = new HashMap<>();
            champs.put("description", "Correction autorisée");
            champs.put("id", autre.getId().toString());
            champs.put("equipement.id", autreEquipement.getId().toString());
            champs.put("statut", "RESOLU");
            champs.put("prestataire.id", "9223372036854775807");
            champs.put("dateOuverture", "2000-01-01T00:00:00");
            champs.put("dateResolution", "2000-01-02T00:00:00");
            champs.put("_csrf", csrf(get(client, detail + "/modifier").body()));
            assertRedirect(post(client, detail, champs), detail);
        }
        Incident modifie = incidents.rechercherDetail(incident.getId()).orElseThrow();
        assertEquals("Correction autorisée", modifie.getDescription());
        assertEquals(equipement.getId(), modifie.getEquipement().getId());
        assertEquals(prestataire.getId(), modifie.getPrestataire().getId());
        assertEquals(StatutIncident.AFFECTE, modifie.getStatut());
        assertEquals(MAINTENANT.minusDays(1), modifie.getDateOuverture());
        assertNull(modifie.getDateResolution());
        assertEquals("Ne pas toucher", incidents.findById(autre.getId()).orElseThrow().getDescription());
        assertEquals(2, incidents.count());
    }

    @Test
    void laConnexionEtLeJetonCsrfRestentObligatoires() throws Exception {
        var equipement = creerEquipement("CSRF-01");
        Incident incident = incidents.save(new Incident(equipement, "Intact", MAINTENANT));
        String detail = "/incidents/" + incident.getId();
        try (HttpClient visiteur = browser()) {
            assertRedirect(get(visiteur, detail + "/modifier"), "/login");
            assertRedirect(post(visiteur, detail, Map.of("description", "Interdit",
                    "_csrf", csrf(get(visiteur, "/login").body()))), "/login");
        }
        try (HttpClient client = navigateurConnecte(Profil.RESPONSABLE_EXPLOITATION)) {
            assertEquals(403, post(client, detail, Map.of("description", "Sans jeton")).statusCode());
            assertEquals(403, post(client, detail, Map.of("description", "Faux jeton", "_csrf", "invalide")).statusCode());
        }
        assertEquals("Intact", incidents.findById(incident.getId()).orElseThrow().getDescription());
    }

    @Test
    void lesIncidentsAbsentsEtIdentifiantsInvalidesNeCreentJamaisDeLigne() throws Exception {
        try (HttpClient client = navigateurConnecte(Profil.RESPONSABLE_EXPLOITATION)) {
            String jeton = csrf(get(client, "/").body());
            assertEquals(404, get(client, "/incidents/9223372036854775807/modifier").statusCode());
            for (String description : new String[]{"Correction", ""}) {
                assertEquals(404, post(client, "/incidents/9223372036854775807",
                        Map.of("description", description, "_csrf", jeton)).statusCode());
            }
            assertEquals(400, get(client, "/incidents/invalide/modifier").statusCode());
            assertEquals(400, post(client, "/incidents/invalide", Map.of("description", "Correction", "_csrf", jeton)).statusCode());
        }
        assertEquals(0, incidents.count());
    }

    @Test
    void laLimiteExacteEtLeTexteHtmlSontAcceptesSansExecution() throws Exception {
        var equipement = creerEquipement("LONGUEUR-01");
        Incident incident = incidents.save(new Incident(equipement, "À corriger", MAINTENANT));
        String detail = "/incidents/" + incident.getId();
        try (HttpClient client = navigateurConnecte(Profil.RESPONSABLE_EXPLOITATION)) {
            for (String description : List.of("x".repeat(Incident.LONGUEUR_DESCRIPTION_MAX), "<script>correction</script>")) {
                assertRedirect(post(client, detail, Map.of("description", description,
                        "_csrf", csrf(get(client, detail + "/modifier").body()))), detail);
                assertEquals(description, incidents.findById(incident.getId()).orElseThrow().getDescription());
            }
            String html = get(client, detail).body();
            assertFalse(html.contains("<script>"));
            assertTrue(html.contains("&lt;script&gt;correction&lt;/script&gt;"));
        }
        assertEquals(1, incidents.count());
    }

    @Test
    void laSecuriteEtLaValidationSappliquentAussiAuxAppelsDirectsDuService() {
        var equipement = creerEquipement("SERVICE-01");
        Incident incident = incidents.save(new Incident(equipement, "Intact", MAINTENANT));
        DescriptionIncident modification = new DescriptionIncident();
        modification.setDescription("Correction");
        SecurityContextHolder.clearContext();
        assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> incidentService.modifierDescription(incident.getId(), modification));
        comme(Profil.AGENT, () -> {
            assertThrows(AccessDeniedException.class, () -> incidentService.modifierDescription(incident.getId(), modification));
            assertThrows(AccessDeniedException.class, () -> incidentService.preparerModification(incident.getId()));
        });
        comme(Profil.RESPONSABLE_EXPLOITATION, () -> {
            modification.setDescription(" ");
            assertThrows(ConstraintViolationException.class, () -> incidentService.modifierDescription(incident.getId(), modification));
            modification.setDescription("x".repeat(Incident.LONGUEUR_DESCRIPTION_MAX + 1));
            assertThrows(ConstraintViolationException.class, () -> incidentService.modifierDescription(incident.getId(), modification));
            assertThrows(ConstraintViolationException.class, () -> incidentService.modifierDescription(incident.getId(), null));
        });
        assertEquals("Intact", incidents.findById(incident.getId()).orElseThrow().getDescription());
    }

    private void comme(Profil profil, Runnable action) {
        var contexte = SecurityContextHolder.createEmptyContext();
        contexte.setAuthentication(UsernamePasswordAuthenticationToken.authenticated("test-service", "",
                List.of(new SimpleGrantedAuthority("ROLE_" + profil.name()))));
        SecurityContextHolder.setContext(contexte);
        try {
            action.run();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
