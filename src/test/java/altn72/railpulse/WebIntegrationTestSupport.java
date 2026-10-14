package altn72.railpulse;

import altn72.railpulse.modele.*;
import altn72.railpulse.service.ProgrammeurService;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(useMainMethod = SpringBootTest.UseMainMethod.ALWAYS,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=${TEST_DB_URL:jdbc:h2:mem:auth-tests;MODE=PostgreSQL;DB_CLOSE_DELAY=-1}",
                "spring.datasource.driver-class-name=${TEST_DB_DRIVER:org.h2.Driver}",
                "spring.datasource.username=${TEST_DB_USERNAME:sa}", "spring.datasource.password=${TEST_DB_PASSWORD:}",
                "spring.jpa.properties.hibernate.default_schema=${TEST_DB_SCHEMA:public}",
                "spring.sql.init.mode=${TEST_DB_INIT:never}",
                "spring.sql.init.schema-locations=classpath:db/supabase-schema.sql",
                "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false"
        })
abstract class WebIntegrationTestSupport {
    protected static final String PASSWORD = "mot-de-passe-test";
    protected final List<User> testUsers = new ArrayList<>();

    @Autowired
    protected UserRepository users;

    @Autowired
    protected PasswordEncoder passwords;

    @Autowired
    protected ProgrammeurRepository programmeurs;

    @Autowired
    protected ProgrammeurService programmeurService;

    protected final List<Programmeur> testProgrammeurs = new ArrayList<>();

    @Value("${local.server.port}")
    protected int port;

    @AfterEach
    void removeTestUsers() {
        programmeurs.deleteAll(testProgrammeurs);
        users.deleteAll(testUsers);
    }


    protected Programmeur createProgrammeur(String nom, String prenom) {
        Programmeur programmeur = new Programmeur();
        programmeur.setNom(nom);
        programmeur.setPrenom(prenom);
        Programmeur saved = programmeurs.save(programmeur);
        testProgrammeurs.add(saved);
        return saved;
    }

    protected HttpClient authenticatedBrowser() throws Exception {
        User user = createUser(Profil.AGENT, passwords.encode(PASSWORD));
        HttpClient client = browser();
        assertRedirect(login(client, user.getLogin(), PASSWORD), "/");
        return client;
    }

    protected User createUser(Profil profil, String password) {
        User user = users.save(new User("test-" + UUID.randomUUID(), password, "Camille", profil));
        testUsers.add(user);
        return user;
    }

    protected HttpClient browser() {
        return HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
    }

    protected String sessionId(HttpClient client) {
        return ((CookieManager) client.cookieHandler().orElseThrow()).getCookieStore().getCookies().stream()
                .filter(cookie -> cookie.getName().equals("JSESSIONID")).findFirst().orElseThrow().getValue();
    }

    protected String csrf(String html) {
        var match = Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(html);
        assertTrue(match.find(), "Le formulaire doit contenir un jeton CSRF");
        return match.group(1);
    }

    protected HttpResponse<String> login(HttpClient client, String username, String password) throws Exception {
        return post(client, "/login", Map.of("username", username, "password", password,
                "_csrf", csrf(get(client, "/login").body())));
    }

    protected HttpResponse<String> post(HttpClient client, String path, Map<String, String> fields) throws Exception {
        String body = fields.entrySet().stream().map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8)
                + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8)).collect(Collectors.joining("&"));
        return client.send(HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }

    protected void assertRedirect(HttpResponse<String> response, String path) {
        assertEquals(302, response.statusCode());
        assertEquals(uri(path).toString(), response.headers().firstValue("location").orElseThrow());
    }

    protected URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    protected HttpResponse<String> get(HttpClient client, String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                        .timeout(Duration.ofSeconds(10)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

}
