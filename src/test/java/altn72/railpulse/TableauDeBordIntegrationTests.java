package altn72.railpulse;

import altn72.railpulse.modele.Incident;
import altn72.railpulse.modele.Prestataire;
import altn72.railpulse.modele.Profil;
import altn72.railpulse.service.FiltrePriseEnCharge;
import altn72.railpulse.service.NiveauAttention;
import altn72.railpulse.service.TableauDeBordService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.net.http.HttpClient;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TableauDeBordIntegrationTests extends SuiviIncidentIntegrationTestSupport {
    @Autowired
    private TableauDeBordService tableauDeBordService;

    @Test
    void lesSeuilsSontCalculesALaSecondePresEtUnAffecteResteDistinct() {
        var equipement = creerEquipement("URGENCE-01");
        assertEquals(NiveauAttention.VERT, NiveauAttention.calculer(
                new Incident(equipement, "Vert", MAINTENANT.minusHours(48).plusSeconds(1)), MAINTENANT));
        assertEquals(NiveauAttention.ORANGE, NiveauAttention.calculer(
                new Incident(equipement, "Orange", MAINTENANT.minusHours(48)), MAINTENANT));
        assertEquals(NiveauAttention.ORANGE, NiveauAttention.calculer(
                new Incident(equipement, "Orange", MAINTENANT.minusHours(72).plusSeconds(1)), MAINTENANT));
        assertEquals(NiveauAttention.ROUGE, NiveauAttention.calculer(
                new Incident(equipement, "Rouge", MAINTENANT.minusHours(72)), MAINTENANT));
        Incident affecte = new Incident(equipement, "Pris en charge", MAINTENANT.minusDays(5));
        affecte.affecter(new Prestataire("Maintenance", "contact@example.invalid"));
        assertEquals(NiveauAttention.PRIS_EN_CHARGE, NiveauAttention.calculer(affecte, MAINTENANT));

        Incident evolutif = new Incident(equipement, "Évolutif", MAINTENANT.minusHours(47));
        assertEquals(NiveauAttention.VERT, NiveauAttention.calculer(evolutif, MAINTENANT));
        assertEquals(NiveauAttention.ORANGE, NiveauAttention.calculer(evolutif, MAINTENANT.plusHours(1)));
        assertEquals(NiveauAttention.ROUGE, NiveauAttention.calculer(evolutif, MAINTENANT.plusHours(25)));
    }

    @Test
    void leResponsableVoitLesActifsParPrioriteAvecIndicateursEtLiens() throws Exception {
        var equipement = creerEquipement("BORD-01");
        var autreEquipement = creerEquipement("BORD-02");
        Prestataire prestataire = prestataires.save(new Prestataire("Maintenance", "contact@example.invalid"));
        Incident vert = incidents.save(new Incident(equipement, "Signalement vert", MAINTENANT.minusHours(47)));
        Incident orange = incidents.save(new Incident(autreEquipement, "Signalement orange", MAINTENANT.minusHours(48)));
        Incident rouge = incidents.save(new Incident(equipement, "Signalement rouge", MAINTENANT.minusHours(72)));
        Incident affecte = new Incident(equipement, "Diagnostic en cours", MAINTENANT.minusDays(5));
        affecte.affecter(prestataire);
        incidents.save(affecte);
        Incident resolu = new Incident(autreEquipement, "Ancien incident résolu", MAINTENANT.minusDays(3));
        resolu.resoudre(MAINTENANT.minusDays(1));
        incidents.save(resolu);

        try (HttpClient client = navigateurConnecte(Profil.RESPONSABLE_EXPLOITATION)) {
            assertTrue(get(client, "/").body().contains("href=\"/tableau-de-bord\""));
            var reponse = get(client, "/tableau-de-bord");
            assertEquals(200, reponse.statusCode());
            String html = reponse.body();
            for (String niveau : List.of("VERT", "ORANGE", "ROUGE", "PRIS_EN_CHARGE")) {
                assertTrue(html.contains("data-niveau=\"" + niveau + "\""));
            }
            assertTrue(html.indexOf("Signalement rouge") < html.indexOf("Signalement orange"));
            assertTrue(html.indexOf("Signalement orange") < html.indexOf("Signalement vert"));
            assertTrue(html.indexOf("Signalement vert") < html.indexOf("Diagnostic en cours"));
            assertFalse(html.contains("Ancien incident résolu"));
            assertTrue(html.contains("href=\"/incidents/" + rouge.getId() + "\""));
            assertTrue(html.contains("href=\"/equipements/" + equipement.getId() + "\""));
            assertTrue(html.contains("BORD-01"));
            assertTrue(html.contains("BORD-02"));
            assertTrue(html.contains("Maintenance"));
            assertTrue(html.contains("name=\"stationId\""));
            assertTrue(html.contains("name=\"priseEnCharge\""));
        }
        var tableau = commeResponsable(() -> tableauDeBordService.consulter(null, FiltrePriseEnCharge.TOUS));
        assertEquals(4, tableau.total());
        assertEquals(3, tableau.enAttente());
        assertEquals(1, tableau.prisEnCharge());
        assertEquals(1, tableau.urgents());
        assertEquals(List.of(rouge.getId(), orange.getId(), vert.getId(), affecte.getId()),
                tableau.incidents().stream().map(ligne -> ligne.incident().getId()).toList());
    }

    @Test
    void lesDeuxFiltresSeCombinentEtLesIndicateursSuiventLaSelection() throws Exception {
        var equipement = creerEquipement("FILTRE-01");
        var autreEquipement = creerEquipement("FILTRE-02");
        Prestataire prestataire = prestataires.save(new Prestataire("Maintenance", "contact@example.invalid"));
        incidents.save(new Incident(equipement, "Attente station un", MAINTENANT.minusHours(49)));
        Incident affecte = new Incident(equipement, "Pris en charge station un", MAINTENANT.minusHours(73));
        affecte.affecter(prestataire);
        incidents.save(affecte);
        incidents.save(new Incident(autreEquipement, "Attente station deux", MAINTENANT.minusHours(80)));

        var selection = commeResponsable(() -> tableauDeBordService.consulter(equipement.getStation().getId(),
                FiltrePriseEnCharge.NON_AFFECTE));
        assertEquals(1, selection.total());
        assertEquals(1, selection.enAttente());
        assertEquals(0, selection.prisEnCharge());
        assertEquals(0, selection.urgents());
        try (HttpClient client = navigateurConnecte(Profil.RESPONSABLE_EXPLOITATION)) {
            String url = "/tableau-de-bord?stationId=" + equipement.getStation().getId() + "&priseEnCharge=NON_AFFECTE";
            String html = get(client, url).body();
            assertTrue(html.contains("Attente station un"));
            assertFalse(html.contains("Pris en charge station un"));
            assertFalse(html.contains("Attente station deux"));
            assertTrue(html.contains("data-niveau=\"ORANGE\""));
            String affectes = get(client, "/tableau-de-bord?priseEnCharge=AFFECTE").body();
            assertTrue(affectes.contains("Pris en charge station un"));
            assertFalse(affectes.contains("Attente station un"));
            assertFalse(affectes.contains("data-niveau=\"ROUGE\""));
            assertTrue(get(client, "/tableau-de-bord?stationId=9223372036854775807").body()
                    .contains("Aucun incident actif pour cette sélection."));
        }
    }

    @Test
    void laPageVideEtLesParametresInvalidesSontTraitesProprement() throws Exception {
        try (HttpClient client = navigateurConnecte(Profil.RESPONSABLE_EXPLOITATION)) {
            var vide = get(client, "/tableau-de-bord");
            assertEquals(200, vide.statusCode());
            assertTrue(vide.body().contains("Aucun incident actif pour cette sélection."));
            assertEquals(400, get(client, "/tableau-de-bord?stationId=invalide").statusCode());
            assertEquals(400, get(client, "/tableau-de-bord?priseEnCharge=RESOLU").statusCode());
        }
    }

    @Test
    void seulsLesResponsablesAccedentAuTableauDeBordMemeViaLeService() throws Exception {
        try (HttpClient visiteur = browser()) {
            assertRedirect(get(visiteur, "/tableau-de-bord"), "/login");
        }
        try (HttpClient agent = navigateurConnecte(Profil.AGENT)) {
            assertFalse(get(agent, "/").body().contains("href=\"/tableau-de-bord\""));
            assertEquals(403, get(agent, "/tableau-de-bord").statusCode());
        }
        SecurityContextHolder.clearContext();
        assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> tableauDeBordService.consulter(null, FiltrePriseEnCharge.TOUS));
        var contexte = SecurityContextHolder.createEmptyContext();
        contexte.setAuthentication(UsernamePasswordAuthenticationToken.authenticated("agent", "",
                List.of(new SimpleGrantedAuthority("ROLE_AGENT"))));
        SecurityContextHolder.setContext(contexte);
        try {
            assertThrows(AccessDeniedException.class,
                    () -> tableauDeBordService.consulter(null, FiltrePriseEnCharge.TOUS));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void lesTextesSontEchappesDansLeTableau() throws Exception {
        var equipement = creerEquipement("BORD-XSS");
        Incident incident = new Incident(equipement, "<script>description</script>", MAINTENANT.minusHours(2));
        incidents.save(incident);
        try (HttpClient client = navigateurConnecte(Profil.RESPONSABLE_EXPLOITATION)) {
            String html = get(client, "/tableau-de-bord").body();
            assertFalse(html.contains("<script>"));
            assertTrue(html.contains("&lt;script&gt;description&lt;/script&gt;"));
        }
    }

    private <T> T commeResponsable(java.util.function.Supplier<T> action) {
        var contexte = SecurityContextHolder.createEmptyContext();
        contexte.setAuthentication(UsernamePasswordAuthenticationToken.authenticated("responsable", "",
                List.of(new SimpleGrantedAuthority("ROLE_RESPONSABLE_EXPLOITATION"))));
        SecurityContextHolder.setContext(contexte);
        try {
            return action.get();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
