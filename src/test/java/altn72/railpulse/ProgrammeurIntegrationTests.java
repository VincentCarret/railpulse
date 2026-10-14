package altn72.railpulse;

import altn72.railpulse.modele.Programmeur;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.web.util.HtmlUtils;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ProgrammeurIntegrationTests extends WebIntegrationTestSupport {
    @Test
    void crudPersistsChangesAndShowsSuccessMessagesWithoutTrustingSubmittedIds() throws Exception {
        Programmeur protectedRecord = createProgrammeur("À conserver", "Exemple");
        try (HttpClient client = authenticatedBrowser()) {
            Map<String, String> fields = new HashMap<>(Map.of("nom", "Création", "prenom", "Camille",
                    "adresse", "12 rue du Test", "langageExpertise", "Java", "livrePrefere", "Clean Code", "salaire", "1200.25",
                    "id", protectedRecord.getId().toString(), "_csrf", csrf(get(client, "/programmeurs/nouveau").body())));
            assertRedirect(post(client, "/programmeurs", fields), "/programmeurs");
            assertTrue(get(client, "/programmeurs").body().contains("Le programmeur a été ajouté."));
            Programmeur created = programmeurs.findAll().stream().filter(p -> p.getNom().equals("Création")).findFirst().orElseThrow();
            testProgrammeurs.add(created);
            assertNotEquals(protectedRecord.getId(), created.getId());
            assertEquals("À conserver", programmeurs.findById(protectedRecord.getId()).orElseThrow().getNom());
            assertEquals("12 rue du Test", created.getAdresse());
            assertEquals("Java", created.getLangageExpertise());
            assertEquals("Clean Code", created.getLivrePrefere());
            assertEquals(1200.25, created.getSalaire());

            fields.put("_method", "PUT");
            fields.put("nom", "Modification");
            fields.put("salaire", "1500");
            fields.put("_csrf", csrf(get(client, "/programmeurs/" + created.getId()).body()));
            assertRedirect(post(client, "/programmeurs/" + created.getId(), fields), "/programmeurs");
            assertTrue(get(client, "/programmeurs").body().contains("Les modifications ont été enregistrées."));
            Programmeur updated = programmeurs.findById(created.getId()).orElseThrow();
            assertEquals("Modification", updated.getNom());
            assertEquals(1500.0, updated.getSalaire());
            assertEquals("À conserver", programmeurs.findById(protectedRecord.getId()).orElseThrow().getNom());

            String token = csrf(get(client, "/programmeurs").body());
            assertRedirect(post(client, "/programmeurs/" + created.getId(), Map.of("_method", "DELETE", "_csrf", token)), "/programmeurs");
            assertTrue(get(client, "/programmeurs").body().contains("Le programmeur a été supprimé."));
            assertTrue(programmeurs.findById(created.getId()).isEmpty());
            assertTrue(programmeurs.findById(protectedRecord.getId()).isPresent());
        }
    }

    @Test
    void invalidCreationsRedisplayTheFormAndNeverWriteToTheDatabase() throws Exception {
        List<Map<String, String>> invalidFields = List.of(
                Map.of("nom", "   "), Map.of("prenom", ""), Map.of("nom", "N".repeat(101)),
                Map.of("prenom", "P".repeat(101)), Map.of("adresse", "A".repeat(256)),
                Map.of("langageExpertise", "J".repeat(101)), Map.of("livrePrefere", "L".repeat(151)),
                Map.of("salaire", "-1"), Map.of("salaire", "pas-un-nombre"), Map.of("salaire", "NaN"),
                Map.of("salaire", "Infinity"));
        try (HttpClient client = authenticatedBrowser()) {
            long count = programmeurs.count();
            for (Map<String, String> invalid : invalidFields) {
                Map<String, String> fields = new HashMap<>(Map.of("nom", "Dupont", "prenom", "Camille"));
                fields.putAll(invalid);
                fields.put("_csrf", csrf(get(client, "/programmeurs/nouveau").body()));
                HttpResponse<String> response = post(client, "/programmeurs", fields);
                assertEquals(200, response.statusCode(), invalid.toString());
                assertTrue(response.body().contains("Aucune modification n’a été enregistrée"), invalid.toString());
                assertTrue(response.body().contains("is-invalid"), invalid.toString());
                assertEquals(count, programmeurs.count(), invalid.toString());
                if (invalid.containsKey("salaire") && invalid.get("salaire").equals("pas-un-nombre")) {
                    assertTrue(response.body().contains("Le salaire doit être un nombre valide."));
                }
            }
        }
    }

    @Test
    void invalidUpdatePreservesTheDatabaseAndTheEnteredValues() throws Exception {
        Programmeur existing = createProgrammeur("Original", "Camille");
        try (HttpClient client = authenticatedBrowser()) {
            String path = "/programmeurs/" + existing.getId();
            HttpResponse<String> response = post(client, path, Map.of("_method", "PUT", "nom", "Nom conservé à l’écran",
                    "prenom", "Camille", "salaire", "-10", "_csrf", csrf(get(client, path).body())));
            assertEquals(200, response.statusCode());
            assertTrue(HtmlUtils.htmlUnescape(response.body()).contains("Nom conservé à l’écran"));
            assertTrue(response.body().contains("Le salaire doit être positif ou nul."));
            assertTrue(response.body().contains("action=\"" + path + "\""));
            assertEquals("Original", programmeurs.findById(existing.getId()).orElseThrow().getNom());
        }
    }

    @Test
    void missingRecordsAndMalformedIdsHaveUsefulErrorPages() throws Exception {
        try (HttpClient client = authenticatedBrowser()) {
            HttpResponse<String> missing = get(client, "/programmeurs/2147483647");
            assertEquals(404, missing.statusCode());
            assertTrue(HtmlUtils.htmlUnescape(missing.body()).contains("n'existe pas"));
            assertTrue(missing.body().contains("Bonjour Camille (AGENT)"));
            HttpResponse<String> malformed = get(client, "/programmeurs/pas-un-id");
            assertEquals(400, malformed.statusCode());
            assertTrue(malformed.body().contains("Les informations transmises ne sont pas valides."));
            for (String method : List.of("PUT", "DELETE")) {
                assertEquals(404, post(client, "/programmeurs/2147483647", Map.of("_method", method, "nom", "Nom", "prenom", "Camille",
                        "_csrf", csrf(get(client, "/programmeurs/nouveau").body()))).statusCode());
            }
        }
    }

    @Test
    void emptyListHasAnExplanationAndACreateAction() throws Exception {
        try (HttpClient client = authenticatedBrowser()) {
            assertEquals(0, programmeurs.count());
            String html = get(client, "/programmeurs").body();
            assertTrue(html.contains("Aucun programmeur pour le moment"));
            assertTrue(html.contains("href=\"/programmeurs/nouveau\""));
        }
    }

    @Test
    void unknownPagesAndUnsupportedMethodsKeepTheirHttpStatus() throws Exception {
        try (HttpClient client = authenticatedBrowser()) {
            assertEquals(404, get(client, "/page-inconnue").statusCode());
            String token = csrf(get(client, "/programmeurs/nouveau").body());
            HttpResponse<String> unsupported = post(client, "/programmeurs", Map.of("_method", "PUT", "_csrf", token));
            assertEquals(405, unsupported.statusCode());
            assertTrue(HtmlUtils.htmlUnescape(unsupported.body()).contains("Cette action n'est pas disponible"));
        }
    }

    @Test
    void crudRequestsWithoutCsrfCannotChangeTheDatabase() throws Exception {
        Programmeur existing = createProgrammeur("Conservé", "Camille");
        try (HttpClient client = authenticatedBrowser()) {
            assertEquals(403, post(client, "/programmeurs", Map.of("nom", "Nouveau", "prenom", "Camille")).statusCode());
            for (String method : List.of("PUT", "DELETE")) {
                assertEquals(403, post(client, "/programmeurs/" + existing.getId(),
                        Map.of("_method", method, "nom", "Modifié", "prenom", "Camille")).statusCode());
            }
            assertEquals(1, programmeurs.count());
            assertEquals("Conservé", programmeurs.findById(existing.getId()).orElseThrow().getNom());
        }
    }

    @Test
    void serviceValidationCannotBeBypassedByCallingTheServiceDirectly() {
        Programmeur invalid = new Programmeur();
        invalid.setNom(" ");
        invalid.setPrenom("Camille");
        assertThrows(ConstraintViolationException.class, () -> programmeurService.ajouterProgrammeur(invalid));
        assertEquals(0, programmeurs.count());
    }

    @Test
    void optionalFieldsAndZeroSalaryAreAccepted() throws Exception {
        try (HttpClient client = authenticatedBrowser()) {
            for (String salary : List.of("", "0")) {
                assertRedirect(post(client, "/programmeurs", Map.of("nom", "Optionnel", "prenom", "Camille", "salaire", salary,
                        "_csrf", csrf(get(client, "/programmeurs/nouveau").body()))), "/programmeurs");
            }
            testProgrammeurs.addAll(programmeurs.findAll());
            assertEquals(2, programmeurs.count());
        }
    }

}
