package altn72.railpulse;

import altn72.railpulse.modele.Profil;
import altn72.railpulse.modele.User;
import altn72.railpulse.service.ComptesDemonstrationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

class ComptesDemonstrationIntegrationTests extends WebIntegrationTestSupport {
    @Autowired
    private ComptesDemonstrationService comptes;

    @AfterEach
    void supprimerComptesDemoDeTest() {
        users.findByLogin(ComptesDemonstrationService.LOGIN_AGENT).ifPresent(users::delete);
        users.findByLogin(ComptesDemonstrationService.LOGIN_RESPONSABLE).ifPresent(users::delete);
    }

    @Test
    void lesComptesSontCreesSansEcraserLesSecretsExistants() {
        comptes.initialiser(PASSWORD, PASSWORD);
        User agent = users.findByLogin(ComptesDemonstrationService.LOGIN_AGENT).orElseThrow();
        User responsable = users.findByLogin(ComptesDemonstrationService.LOGIN_RESPONSABLE).orElseThrow();
        assertEquals(Profil.AGENT, agent.getProfil());
        assertEquals(Profil.RESPONSABLE_EXPLOITATION, responsable.getProfil());
        assertTrue(passwords.matches(PASSWORD, agent.getPassword()));
        String empreinte = agent.getPassword();
        comptes.initialiser(null, null);
        assertEquals(empreinte, users.findById(agent.getId()).orElseThrow().getPassword());
    }

    @Test
    void unSecretManquantAnnuleLaCreationDesDeuxComptes() {
        assertThrows(IllegalStateException.class, () -> comptes.initialiser(PASSWORD, null));
        assertTrue(users.findByLogin(ComptesDemonstrationService.LOGIN_AGENT).isEmpty());
        assertTrue(users.findByLogin(ComptesDemonstrationService.LOGIN_RESPONSABLE).isEmpty());
    }

    @Test
    void unLoginExistantAvecUnAutreProfilNestPasReaffecte() {
        User existant = users.save(new User(ComptesDemonstrationService.LOGIN_AGENT,
                passwords.encode(PASSWORD), "Existant", Profil.RESPONSABLE_EXPLOITATION));
        assertThrows(IllegalStateException.class, () -> comptes.initialiser(PASSWORD, PASSWORD));
        assertEquals(Profil.RESPONSABLE_EXPLOITATION, users.findById(existant.getId()).orElseThrow().getProfil());
    }
}
