package altn72.railpulse;

import altn72.railpulse.modele.Profil;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;

import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Import(SuiviIncidentIntegrationTestSupport.HorlogeTest.class)
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:suivi-tests;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
abstract class SuiviIncidentIntegrationTestSupport extends CatalogueIntegrationTestSupport {
    protected static final LocalDateTime MAINTENANT = LocalDateTime.of(2026, 10, 27, 12, 0);

    @TestConfiguration(proxyBeanMethods = false)
    static class HorlogeTest {
        @Bean
        @Primary
        Clock horlogeFixe() {
            return Clock.fixed(Instant.parse("2026-10-27T12:00:00Z"), ZoneOffset.UTC);
        }
    }

    protected HttpClient navigateurConnecte(Profil profil) throws Exception {
        var utilisateur = createUser(profil, passwords.encode(PASSWORD));
        HttpClient client = browser();
        assertRedirect(login(client, utilisateur.getLogin(), PASSWORD), "/");
        return client;
    }
}
