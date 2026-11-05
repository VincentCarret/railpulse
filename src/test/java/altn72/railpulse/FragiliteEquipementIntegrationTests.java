package altn72.railpulse;

import altn72.railpulse.modele.Equipement;
import altn72.railpulse.modele.Incident;
import altn72.railpulse.modele.Prestataire;
import altn72.railpulse.modele.Profil;
import altn72.railpulse.service.EquipementService;
import altn72.railpulse.service.FragiliteEquipementService;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.util.HtmlUtils;

import static org.junit.jupiter.api.Assertions.*;

@Import(FragiliteEquipementIntegrationTests.ConfigurationHorloge.class)
@TestPropertySource(properties = {
        "spring.datasource.url=${TEST_DB_URL:jdbc:h2:mem:fragilite-tests;MODE=PostgreSQL;DB_CLOSE_DELAY=-1}",
        "spring.jpa.properties.hibernate.default_schema=${TEST_DB_SCHEMA_SUIVI:public}"
})
class FragiliteEquipementIntegrationTests extends CatalogueIntegrationTestSupport {
    private static final Instant INSTANT = Instant.parse("2026-11-05T12:00:00Z");
    private static final LocalDateTime MAINTENANT = LocalDateTime.ofInstant(INSTANT, ZoneOffset.UTC);
    private static final String BADGE = ">Équipement fragile</span>";

    @Autowired private FragiliteEquipementService fragilite;
    @Autowired private EquipementService catalogue;
    @Autowired private HorlogeMutable horloge;

    @BeforeEach
    void remettreHorloge() {
        horloge.instant.set(INSTANT);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ConfigurationHorloge {
        @Bean @Primary
        HorlogeMutable horlogeFragilite() {
            return new HorlogeMutable(new AtomicReference<>(INSTANT), ZoneOffset.UTC);
        }
    }

    static class HorlogeMutable extends Clock {
        private final AtomicReference<Instant> instant;
        private final ZoneId zone;

        HorlogeMutable(AtomicReference<Instant> instant, ZoneId zone) {
            this.instant = instant;
            this.zone = zone;
        }

        @Override public ZoneId getZone() { return zone; }
        @Override public Clock withZone(ZoneId zone) { return new HorlogeMutable(instant, zone); }
        @Override public Instant instant() { return instant.get(); }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4})
    void leSeuilEstInclusif(int nombre) {
        var equipement = creerEquipement("SEUIL-" + nombre);
        for (int i = 0; i < nombre; i++) {
            ajouter(equipement, MAINTENANT.minusDays(i));
        }
        assertEquals(nombre >= 3, fragilite.estFragile(equipement.getId()));
        assertEquals(nombre >= 3, catalogue.consulterEquipement(equipement.getId()).fragile());
    }

    @ParameterizedTest
    @CsvSource({"-1,false", "0,true", "1,true"})
    void laBorneDesTrenteJoursEstInclusiveALaMinutePres(int decalageMinutes, boolean attendu) {
        var equipement = creerEquipement("BORNE");
        ajouter(equipement, MAINTENANT.minusDays(1));
        ajouter(equipement, MAINTENANT.minusDays(2));
        ajouter(equipement, MAINTENANT.minusDays(30).plusMinutes(decalageMinutes));
        assertEquals(attendu, fragilite.estFragile(equipement.getId()));
    }

    @Test
    void tousLesStatutsComptentMemeUnIncidentResolu() {
        var equipement = creerEquipement("STATUTS");
        ajouter(equipement, MAINTENANT.minusDays(1));
        Prestataire prestataire = prestataires.save(new Prestataire("Maintenance", "demo@example.invalid"));
        Incident affecte = new Incident(equipement, "Affecté", MAINTENANT.minusDays(2));
        affecte.affecter(prestataire);
        incidents.save(affecte);
        Incident resolu = new Incident(equipement, "Résolu", MAINTENANT.minusDays(3));
        resolu.resoudre(MAINTENANT.minusDays(1));
        incidents.save(resolu);
        assertTrue(fragilite.estFragile(equipement.getId()));
    }

    @Test
    void lesVieuxIncidentsEtLesEquipementsVoisinsNeContaminentPasLeCalcul() {
        var equipement = creerEquipement("DEUX-RECENTS");
        ajouter(equipement, MAINTENANT.minusDays(1));
        ajouter(equipement, MAINTENANT.minusDays(2));
        for (int i = 0; i < 5; i++) {
            ajouter(equipement, MAINTENANT.minusDays(45));
        }
        var fragile = creerEquipement("VOISIN-FRAGILE");
        for (int i = 0; i < 3; i++) ajouter(fragile, MAINTENANT.minusDays(1));
        var voisin = creerEquipement("VOISIN-UN-SEUL");
        ajouter(voisin, MAINTENANT.minusDays(1));
        assertFalse(fragilite.estFragile(equipement.getId()));
        assertTrue(fragilite.estFragile(fragile.getId()));
        assertFalse(fragilite.estFragile(voisin.getId()));
    }

    @Test
    void laFicheAfficheLeBadgeExactPourLesDeuxProfilsSeulementSiFragile() throws Exception {
        var fragile = creerEquipement("FICHE-FRAGILE");
        for (int i = 0; i < 3; i++) ajouter(fragile, MAINTENANT.minusDays(1));
        var voisin = creerEquipement("FICHE-NORMALE");
        ajouter(voisin, MAINTENANT.minusDays(1));
        for (Profil profil : Profil.values()) {
            try (HttpClient client = connecter(profil)) {
                var fiche = get(client, "/equipements/" + fragile.getId());
                assertEquals(200, fiche.statusCode());
                assertEquals(1, occurrences(fiche.body(), BADGE));
                assertFalse(get(client, "/equipements/" + voisin.getId()).body().contains(BADGE));
            }
        }
    }

    @Test
    void chaqueLigneActiveFragilePorteLeBadgeMemeQuandLeTroisiemeIncidentEstResoluOuFiltre() throws Exception {
        var equipement = creerEquipement("BORD-FRAGILE");
        Incident attente = ajouter(equipement, MAINTENANT.minusDays(1));
        Prestataire prestataire = prestataires.save(new Prestataire("Maintenance", "demo@example.invalid"));
        Incident affecte = new Incident(equipement, "Actif affecté", MAINTENANT.minusDays(2));
        affecte.affecter(prestataire);
        incidents.save(affecte);
        Incident resolu = new Incident(equipement, "Résolu compté", MAINTENANT.minusDays(3));
        resolu.resoudre(MAINTENANT.minusDays(1));
        incidents.save(resolu);
        var voisin = creerEquipement("BORD-NORMAL");
        Incident normal = ajouter(voisin, MAINTENANT.minusDays(1));
        try (HttpClient client = connecter(Profil.RESPONSABLE_EXPLOITATION)) {
            var reponse = get(client, "/tableau-de-bord");
            assertEquals(200, reponse.statusCode());
            String html = reponse.body();
            assertEquals(2, occurrences(html, BADGE));
            assertTrue(ligne(html, attente).contains(BADGE));
            assertTrue(ligne(html, affecte).contains(BADGE));
            assertFalse(ligne(html, normal).contains(BADGE));
            assertFalse(html.contains("href=\"/incidents/" + resolu.getId() + "\""));
            String filtre = get(client, "/tableau-de-bord?priseEnCharge=NON_AFFECTE&stationId="
                    + equipement.getStation().getId()).body();
            assertEquals(1, occurrences(filtre, BADGE));
            assertTrue(ligne(filtre, attente).contains(BADGE));
        }
    }

    @Test
    void leTempsSeulFaitDisparaitreLeBadgeSansModifierLaBase() throws Exception {
        var equipement = creerEquipement("GLISSANTE");
        ajouter(equipement, MAINTENANT.minusDays(1));
        ajouter(equipement, MAINTENANT.minusDays(2));
        ajouter(equipement, MAINTENANT.minusDays(30).plusMinutes(1));
        try (HttpClient client = connecter(Profil.RESPONSABLE_EXPLOITATION)) {
            assertTrue(get(client, "/equipements/" + equipement.getId()).body().contains(BADGE));
            assertEquals(3, occurrences(get(client, "/tableau-de-bord").body(), BADGE));
            horloge.instant.set(INSTANT.plusSeconds(120));
            assertFalse(catalogue.consulterEquipement(equipement.getId()).fragile());
            assertFalse(get(client, "/equipements/" + equipement.getId()).body().contains(BADGE));
            assertFalse(get(client, "/tableau-de-bord").body().contains(BADGE));
            assertEquals(3, incidents.count());
        }
    }

    @Test
    void laConfirmationSignaleFragileDesLaTroisiemeDeclarationEtAuDelaPourLesDeuxProfils() throws Exception {
        for (Profil profil : Profil.values()) {
            var equipement = creerEquipement("DECLARATION-" + profil);
            String fiche = "/equipements/" + equipement.getId();
            try (HttpClient client = connecter(profil)) {
                for (int nombre = 1; nombre <= 4; nombre++) {
                    String jeton = csrf(get(client, fiche + "/incidents/nouveau").body());
                    assertRedirect(post(client, fiche + "/incidents", Map.of("description", "Panne " + nombre,
                            "_csrf", jeton)), fiche);
                    String html = HtmlUtils.htmlUnescape(get(client, fiche).body());
                    assertTrue(html.contains("L'incident a été déclaré."));
                    assertEquals(nombre >= 3, html.contains("Cet équipement est fragile."));
                    assertEquals(nombre >= 3, html.contains(BADGE));
                }
            }
        }
    }

    private Incident ajouter(Equipement equipement, LocalDateTime ouverture) {
        return incidents.save(new Incident(equipement, "Signalement", ouverture));
    }

    private HttpClient connecter(Profil profil) throws Exception {
        var utilisateur = createUser(profil, passwords.encode(PASSWORD));
        HttpClient client = browser();
        assertRedirect(login(client, utilisateur.getLogin(), PASSWORD), "/");
        return client;
    }

    private static long occurrences(String texte, String recherche) {
        return Pattern.compile(Pattern.quote(recherche)).matcher(texte).results().count();
    }

    private static String ligne(String html, Incident incident) {
        return Pattern.compile("<tr>.*?</tr>", Pattern.DOTALL).matcher(html).results()
                .map(resultat -> resultat.group())
                .filter(ligne -> ligne.contains("href=\"/incidents/" + incident.getId() + "\""))
                .findFirst().orElseThrow();
    }
}
