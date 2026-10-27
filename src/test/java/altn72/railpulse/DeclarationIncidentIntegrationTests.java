package altn72.railpulse;

import altn72.railpulse.modele.Incident;
import altn72.railpulse.modele.Profil;
import altn72.railpulse.modele.StatutIncident;
import altn72.railpulse.service.DeclarationIncident;
import altn72.railpulse.service.IncidentService;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.util.HtmlUtils;

import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Import(DeclarationIncidentIntegrationTests.HorlogeTest.class)
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:f4-tests;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
class DeclarationIncidentIntegrationTests extends CatalogueIntegrationTestSupport {
    private static final Instant INSTANT_DECLARATION = Instant.parse("2026-10-27T12:00:00Z");

    @Autowired
    private IncidentService incidentService;

    @TestConfiguration(proxyBeanMethods = false)
    static class HorlogeTest {
        @Bean
        @Primary
        Clock horlogeFixe() {
            return Clock.fixed(INSTANT_DECLARATION, ZoneOffset.UTC);
        }
    }

    @Test
    void lesDeuxProfilsDeclarentUnIncidentDateParLeServeurEtVisibleSurLaFiche() throws Exception {
        var equipement = creerEquipement("DECLARATION-01");
        String fiche = "/equipements/" + equipement.getId();
        String action = fiche + "/incidents";
        for (Profil profil : Profil.values()) {
            var utilisateur = createUser(profil, passwords.encode(PASSWORD));
            try (HttpClient client = browser()) {
                assertRedirect(login(client, utilisateur.getLogin(), PASSWORD), "/");
                assertTrue(get(client, fiche).body().contains("href=\"" + action + "/nouveau\""));
                var formulaire = get(client, action + "/nouveau");
                assertEquals(200, formulaire.statusCode());
                assertTrue(formulaire.body().contains("DECLARATION-01"));
                assertTrue(formulaire.body().contains("République"));
                assertTrue(formulaire.body().contains("Bonjour Camille (" + profil + ")"));
                String description = "Arrêt signalé par " + profil;
                long avant = incidents.count();
                assertRedirect(post(client, action, Map.of("description", "  " + description + "  ",
                        "_csrf", csrf(formulaire.body()))), fiche);
                assertEquals(avant + 1, incidents.count());
                Incident incident = incidents.rechercherParEquipement(equipement.getId()).getFirst();
                assertEquals(description, incident.getDescription());
                assertEquals(equipement.getId(), incident.getEquipement().getId());
                assertEquals(LocalDateTime.ofInstant(INSTANT_DECLARATION, ZoneOffset.UTC), incident.getDateOuverture());
                assertEquals(StatutIncident.NON_AFFECTE, incident.getStatut());
                assertNull(incident.getPrestataire());
                assertNull(incident.getDateResolution());
                String confirmation = HtmlUtils.htmlUnescape(get(client, fiche).body());
                assertTrue(confirmation.contains("L'incident a été déclaré."));
                assertTrue(confirmation.contains(description));
                assertTrue(confirmation.contains("27/10/2026 12:00"));
                assertFalse(HtmlUtils.htmlUnescape(get(client, fiche).body()).contains("L'incident a été déclaré."));
                assertEquals(avant + 1, incidents.count(), "Le rafraîchissement après redirection ne redéclare pas l'incident.");
            }
        }
    }

    @Test
    void lesDonneesIndispensablesSontValideesSansEcritureEtLaSaisieEstConservee() throws Exception {
        var equipement = creerEquipement("INVALIDE-01");
        String action = "/equipements/" + equipement.getId() + "/incidents";
        try (HttpClient client = authenticatedBrowser()) {
            for (Map<String, String> saisie : List.of(Map.<String, String>of(), Map.of("description", ""),
                    Map.of("description", " \n\t "), Map.of("description", "Texte conservé " + "x".repeat(2000)))) {
                Map<String, String> champs = new HashMap<>(saisie);
                champs.put("_csrf", csrf(get(client, action + "/nouveau").body()));
                var reponse = post(client, action, champs);
                assertEquals(200, reponse.statusCode());
                assertTrue(reponse.body().contains("Aucun incident n’a été enregistré."));
                assertTrue(reponse.body().contains("is-invalid"));
                assertTrue(reponse.body().contains("aria-invalid=\"true\""));
                assertEquals(0, incidents.count());
                if (saisie.getOrDefault("description", "").startsWith("Texte conservé")) {
                    assertTrue(HtmlUtils.htmlUnescape(reponse.body()).contains(saisie.get("description")));
                    assertTrue(reponse.body().contains("La description ne doit pas dépasser 2000 caractères."));
                } else {
                    assertTrue(reponse.body().contains("Décrivez le problème rencontré sur cet équipement."));
                }
            }
        }
    }

    @Test
    void lesIdentifiantsEtLEtatEnvoyesParLeClientNePeuventPasModifierLaDeclaration() throws Exception {
        var equipement = creerEquipement("CIBLE-01");
        var autre = creerEquipement("PROTEGE-01");
        Incident existant = incidents.save(new Incident(autre, "À conserver", LocalDateTime.of(2026, 10, 20, 10, 30)));
        String action = "/equipements/" + equipement.getId() + "/incidents";
        try (HttpClient client = authenticatedBrowser()) {
            Map<String, String> champs = new HashMap<>();
            champs.put("description", "Nouveau signalement");
            champs.put("id", existant.getId().toString());
            champs.put("equipementId", autre.getId().toString());
            champs.put("equipement.id", autre.getId().toString());
            champs.put("statut", "RESOLU");
            champs.put("prestataire.id", "999");
            champs.put("dateOuverture", "2000-01-01T00:00:00");
            champs.put("dateResolution", "2000-01-02T00:00:00");
            champs.put("_csrf", csrf(get(client, action + "/nouveau").body()));
            assertRedirect(post(client, action, champs), "/equipements/" + equipement.getId());
            Incident nouveau = incidents.rechercherParEquipement(equipement.getId()).getFirst();
            assertNotEquals(existant.getId(), nouveau.getId());
            assertEquals(equipement.getId(), nouveau.getEquipement().getId());
            assertEquals(StatutIncident.NON_AFFECTE, nouveau.getStatut());
            assertEquals(LocalDateTime.ofInstant(INSTANT_DECLARATION, ZoneOffset.UTC), nouveau.getDateOuverture());
            assertNull(nouveau.getPrestataire());
            assertNull(nouveau.getDateResolution());
            assertEquals("À conserver", incidents.findById(existant.getId()).orElseThrow().getDescription());
            assertEquals(2, incidents.count());
        }
    }

    @Test
    void uneDescriptionDeLongueurMaximaleEtLeTexteHtmlSontAcceptesSansExecution() throws Exception {
        var equipement = creerEquipement("LIMITE-01");
        String action = "/equipements/" + equipement.getId() + "/incidents";
        try (HttpClient client = authenticatedBrowser()) {
            for (String description : List.of("x".repeat(Incident.LONGUEUR_DESCRIPTION_MAX), "<script>alert('signalement')</script>")) {
                assertRedirect(post(client, action, Map.of("description", description,
                        "_csrf", csrf(get(client, action + "/nouveau").body()))), "/equipements/" + equipement.getId());
            }
            assertEquals(2, incidents.count());
            String html = get(client, "/equipements/" + equipement.getId()).body();
            assertFalse(html.contains("<script>"));
            assertTrue(html.contains("&lt;script&gt;"));
        }
    }

    @Test
    void laConnexionEtLeJetonCsrfSontObligatoiresPourDeclarer() throws Exception {
        var equipement = creerEquipement("SECURITE-01");
        String action = "/equipements/" + equipement.getId() + "/incidents";
        try (HttpClient visiteur = browser()) {
            assertRedirect(get(visiteur, action + "/nouveau"), "/login");
            assertRedirect(post(visiteur, action, Map.of("description", "Refusé", "_csrf", csrf(get(visiteur, "/login").body()))), "/login");
        }
        try (HttpClient client = authenticatedBrowser()) {
            assertEquals(403, post(client, action, Map.of("description", "Sans jeton")).statusCode());
            assertEquals(403, post(client, action, Map.of("description", "Faux jeton", "_csrf", "invalide")).statusCode());
        }
        assertEquals(0, incidents.count());
    }

    @Test
    void unEquipementAbsentOuUnIdentifiantInvalideNeCreeAucunIncident() throws Exception {
        String absent = "/equipements/9223372036854775807/incidents";
        try (HttpClient client = authenticatedBrowser()) {
            assertEquals(404, get(client, absent + "/nouveau").statusCode());
            String jeton = csrf(get(client, "/").body());
            assertEquals(404, post(client, absent, Map.of("description", "Panne", "_csrf", jeton)).statusCode());
            assertEquals(404, post(client, absent, Map.of("description", "", "_csrf", jeton)).statusCode());
            assertEquals(400, get(client, "/equipements/invalide/incidents/nouveau").statusCode());
            assertEquals(400, post(client, "/equipements/invalide/incidents", Map.of("description", "Panne", "_csrf", jeton)).statusCode());
        }
        assertEquals(0, incidents.count());
    }

    @Test
    void laValidationDuServiceNePeutPasEtreContournee() {
        var equipement = creerEquipement("SERVICE-01");
        DeclarationIncident declaration = new DeclarationIncident();
        assertThrows(ConstraintViolationException.class, () -> incidentService.declarer(equipement.getId(), declaration));
        declaration.setDescription(" ");
        assertThrows(ConstraintViolationException.class, () -> incidentService.declarer(equipement.getId(), declaration));
        declaration.setDescription("x".repeat(Incident.LONGUEUR_DESCRIPTION_MAX + 1));
        assertThrows(ConstraintViolationException.class, () -> incidentService.declarer(equipement.getId(), declaration));
        assertThrows(ConstraintViolationException.class, () -> incidentService.declarer(equipement.getId(), null));
        assertEquals(0, incidents.count());
    }
}
