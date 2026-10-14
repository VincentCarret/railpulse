package altn72.railpulse;

import altn72.railpulse.modele.*;
import org.junit.jupiter.api.Test;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class RailPulseApplicationTests extends WebIntegrationTestSupport {
    @Test
    void contextLoads() {
    }

    @Test
    void loginPageIsPublicAndProtectedPagesRequireAuthentication() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> login = get(client, "/login");
            assertEquals(200, login.statusCode());
            assertTrue(login.body().contains("Connexion · Rail Pulse"));
            assertTrue(login.body().contains("name=\"_csrf\""));
            assertTrue(login.body().contains("name=\"username\""));
            assertEquals(200, get(client, "/css/login.css").statusCode());
            assertEquals(200, get(client, "/js/login.js").statusCode());
            assertTrue(get(client, "/login?error").body().contains("Identifiant ou mot de passe incorrect"));
            assertTrue(get(client, "/login?logout").body().contains("déconnecté avec succès"));
            HttpResponse<String> protectedPage = get(client, "/programmeurs");
            assertEquals(302, protectedPage.statusCode());
            assertTrue(protectedPage.headers().firstValue("location").orElseThrow().endsWith("/login"));
        }
    }

    @Test
    void invalidCredentialsRedisplayLoginWithAnErrorWithoutOpeningASession() throws Exception {
        User user = createUser(Profil.AGENT, PASSWORD);
        try (HttpClient client = browser()) {
            for (String login : List.of(user.getLogin(), "inconnu")) {
                HttpResponse<String> result = login(client, login, "incorrect");
                assertRedirect(result, "/login?error");
                HttpResponse<String> page = get(client, "/login?error");
                assertEquals(200, page.statusCode());
                assertTrue(page.body().contains("Identifiant ou mot de passe incorrect"));
                assertRedirect(get(client, "/"), "/login");
            }
            assertEquals(PASSWORD, users.findById(user.getId()).orElseThrow().getPassword());
        }
    }

    @Test
    void bothProfilesCanLoginAndLegacyPasswordsAreUpgradedAfterSuccess() throws Exception {
        for (Profil profil : Profil.values()) {
            User user = createUser(profil, PASSWORD);
            try (HttpClient client = browser()) {
                get(client, "/login");
                String oldSession = sessionId(client);
                assertRedirect(login(client, user.getLogin(), PASSWORD), "/");
                assertNotEquals(oldSession, sessionId(client));
                HttpResponse<String> home = get(client, "/");
                assertEquals(200, home.statusCode());
                assertTrue(home.body().contains(user.getPrenom()));
                assertTrue(home.body().contains(profil.name()));
                String storedPassword = users.findById(user.getId()).orElseThrow().getPassword();
                assertTrue(storedPassword.startsWith("{bcrypt}"));
                assertTrue(passwords.matches(PASSWORD, storedPassword));
                assertRedirect(login(client, user.getLogin(), PASSWORD), "/");
            }
        }
    }

    @Test
    void encodedPasswordsCanLogin() throws Exception {
        for (String encoded : List.of(passwords.encode(PASSWORD), passwords.encode(PASSWORD).substring(8))) {
            User user = createUser(Profil.AGENT, encoded);
            try (HttpClient client = browser()) {
                assertRedirect(login(client, user.getLogin(), PASSWORD), "/");
                assertEquals(200, get(client, "/").statusCode());
            }
        }
    }

    @Test
    void logoutClosesTheSessionAndTheOldCookieCannotBeReused() throws Exception {
        User user = createUser(Profil.AGENT, passwords.encode(PASSWORD));
        try (HttpClient client = browser(); HttpClient replay = HttpClient.newHttpClient()) {
            assertRedirect(login(client, user.getLogin(), PASSWORD), "/");
            String session = sessionId(client);
            String token = csrf(get(client, "/").body());
            assertRedirect(post(client, "/logout", Map.of("_csrf", token)), "/login?logout");
            assertTrue(get(client, "/login?logout").body().contains("déconnecté avec succès"));
            assertRedirect(get(client, "/"), "/login");
            assertRedirect(get(client, "/programmeurs"), "/login");
            HttpResponse<String> reused = replay.send(HttpRequest.newBuilder(uri("/"))
                    .header("Cookie", "JSESSIONID=" + session).GET().build(), HttpResponse.BodyHandlers.ofString());
            assertRedirect(reused, "/login");
        }
    }

    @Test
    void loginAndLogoutRequireCsrfTokens() throws Exception {
        User user = createUser(Profil.AGENT, passwords.encode(PASSWORD));
        try (HttpClient client = browser()) {
            assertEquals(403, post(client, "/login", Map.of("username", user.getLogin(), "password", PASSWORD)).statusCode());
            assertRedirect(login(client, user.getLogin(), PASSWORD), "/");
            assertEquals(403, post(client, "/logout", Map.of()).statusCode());
            assertEquals(200, get(client, "/").statusCode());
        }
    }

    @Test
    void everyPrivatePageShowsTheUserAndItsLogoutFormWorksForBothProfiles() throws Exception {
        Programmeur programmeur = new Programmeur();
        programmeur.setNom("Test");
        programmeur.setPrenom("Exemple");
        programmeur = programmeurs.save(programmeur);
        try {
            for (Profil profil : Profil.values()) {
                User user = createUser(profil, passwords.encode(PASSWORD));
                for (String path : List.of("/", "/programmeurs", "/programmeurs/nouveau", "/programmeurs/" + programmeur.getId())) {
                    try (HttpClient client = browser()) {
                        assertRedirect(get(client, path), "/login");
                        assertRedirect(login(client, user.getLogin(), PASSWORD), "/");
                        HttpResponse<String> page = get(client, path);
                        assertEquals(200, page.statusCode(), path);
                        assertTrue(page.body().contains("Bonjour Camille (" + profil.name() + ")"), path);
                        assertTrue(page.body().contains("Déconnectez-vous"), path);
                        assertTrue(page.body().contains("action=\"/logout\""), path);
                        assertEquals(200, get(client, "/css/style.css").statusCode());
                        assertRedirect(post(client, "/logout", Map.of("_csrf", csrf(page.body()))), "/login?logout");
                        assertRedirect(get(client, path), "/login");
                    }
                }
            }
        } finally {
            programmeurs.delete(programmeur);
        }
    }

}
