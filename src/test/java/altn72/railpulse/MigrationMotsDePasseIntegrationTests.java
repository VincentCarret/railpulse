package altn72.railpulse;

import altn72.railpulse.modele.Profil;
import altn72.railpulse.modele.User;
import altn72.railpulse.service.MigrationMotsDePasse;
import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import java.net.http.HttpClient;

import static org.junit.jupiter.api.Assertions.*;

class MigrationMotsDePasseIntegrationTests extends WebIntegrationTestSupport {
    @Autowired
    private MigrationMotsDePasse migration;

    @Autowired
    private EntityManager entites;

    @Autowired
    private TransactionTemplate transactions;

    @Test
    void lesAnciennesValeursSontMigreesSansChangerLesIdentifiantsNiLesProfils() {
        for (String ancienneValeur : new String[]{PASSWORD, "{noop}" + PASSWORD, passwords.encode(PASSWORD).substring(8)}) {
            User utilisateur = createUser(Profil.AGENT, passwords.encode(PASSWORD));
            simulerAncienneValeur(utilisateur, ancienneValeur);
            migration.migrer();
            User migre = users.findById(utilisateur.getId()).orElseThrow();
            assertEquals(utilisateur.getLogin(), migre.getLogin());
            assertEquals(Profil.AGENT, migre.getProfil());
            assertTrue(migre.getPassword().startsWith("{bcrypt}"));
            assertTrue(passwords.matches(PASSWORD, migre.getPassword()));
            String empreinte = migre.getPassword();
            migration.migrer();
            assertEquals(empreinte, users.findById(utilisateur.getId()).orElseThrow().getPassword());
        }
    }

    @Test
    void uneValeurEnClairNestJamaisAccepteeParLecranDeConnexion() throws Exception {
        User utilisateur = createUser(Profil.AGENT, passwords.encode(PASSWORD));
        for (String ancienneValeur : new String[]{PASSWORD, "{noop}" + PASSWORD}) {
            simulerAncienneValeur(utilisateur, ancienneValeur);
            try (HttpClient client = browser()) {
                assertRedirect(login(client, utilisateur.getLogin(), PASSWORD), "/login?error");
                assertRedirect(get(client, "/"), "/login");
                migration.migrer();
                assertRedirect(login(client, utilisateur.getLogin(), PASSWORD), "/");
            }
        }
    }

    @Test
    void laPersistanceRefuseDeNouveauxMotsDePasseNonHaches() {
        User utilisateur = new User("test-hash-obligatoire", PASSWORD, "Camille", Profil.AGENT);
        assertThrows(ConstraintViolationException.class, () -> users.saveAndFlush(utilisateur));
        assertTrue(users.findByLogin(utilisateur.getLogin()).isEmpty());
    }

    @Test
    void unFormatInconnuAnnuleTouteLaMigrationSansExposerLaValeur() {
        User premier = createUser(Profil.AGENT, passwords.encode(PASSWORD));
        User second = createUser(Profil.RESPONSABLE_EXPLOITATION, passwords.encode(PASSWORD));
        simulerAncienneValeur(premier, PASSWORD);
        simulerAncienneValeur(second, "{format-inconnu}" + PASSWORD);
        var erreur = assertThrows(IllegalStateException.class, migration::migrer);
        assertFalse(erreur.getMessage().contains(PASSWORD));
        assertEquals(PASSWORD, users.findById(premier.getId()).orElseThrow().getPassword());
    }

    private void simulerAncienneValeur(User utilisateur, String valeur) {
        // Reproduire une base historique en contournant uniquement dans le test la validation JPA.
        transactions.executeWithoutResult(transaction -> entites.createNativeQuery(
                        "update {h-schema}users set password = :valeur where id = :id")
                .setParameter("valeur", valeur).setParameter("id", utilisateur.getId()).executeUpdate());
    }
}
