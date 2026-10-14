package altn72.railpulse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import altn72.railpulse.modele.Profil;
import altn72.railpulse.modele.User;
import altn72.railpulse.modele.UserRepository;
import altn72.railpulse.modele.Programmeur;
import altn72.railpulse.modele.ProgrammeurRepository;

import java.net.URI;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@SpringBootTest(useMainMethod = SpringBootTest.UseMainMethod.ALWAYS,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:auth-tests;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa", "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false"
        })
class RailPulseApplicationTests {

    private static final String PASSWORD = "mot-de-passe-test";
    private final List<User> testUsers = new ArrayList<>();

    @Autowired
    private UserRepository users;

    @Autowired
    private PasswordEncoder passwords;

    @Autowired
    private ProgrammeurRepository programmeurs;

    @Value("${local.server.port}")
    private int port;

    @AfterEach
    void removeTestUsers() {
        users.deleteAll(testUsers);
    }

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

    private User createUser(Profil profil, String password) {
        User user = users.save(new User("test-" + UUID.randomUUID(), password, "Camille", profil));
        testUsers.add(user);
        return user;
    }

    private HttpClient browser() {
        return HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
    }

    private String sessionId(HttpClient client) {
        return ((CookieManager) client.cookieHandler().orElseThrow()).getCookieStore().getCookies().stream()
                .filter(cookie -> cookie.getName().equals("JSESSIONID")).findFirst().orElseThrow().getValue();
    }

    private String csrf(String html) {
        var match = Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(html);
        assertTrue(match.find(), "Le formulaire doit contenir un jeton CSRF");
        return match.group(1);
    }

    private HttpResponse<String> login(HttpClient client, String username, String password) throws Exception {
        return post(client, "/login", Map.of("username", username, "password", password,
                "_csrf", csrf(get(client, "/login").body())));
    }

    private HttpResponse<String> post(HttpClient client, String path, Map<String, String> fields) throws Exception {
        String body = fields.entrySet().stream().map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8)
                + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8)).collect(Collectors.joining("&"));
        return client.send(HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }

    private void assertRedirect(HttpResponse<String> response, String path) {
        assertEquals(302, response.statusCode());
        assertEquals(uri(path).toString(), response.headers().firstValue("location").orElseThrow());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private HttpResponse<String> get(HttpClient client, String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                        .timeout(Duration.ofSeconds(10)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

}
