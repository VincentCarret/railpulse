package altn72.railpulse;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Value;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(useMainMethod = SpringBootTest.UseMainMethod.ALWAYS,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RailPulseApplicationTests {

    @Value("${local.server.port}")
    private int port;

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

    private HttpResponse<String> get(HttpClient client, String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                        .timeout(Duration.ofSeconds(10)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

}
