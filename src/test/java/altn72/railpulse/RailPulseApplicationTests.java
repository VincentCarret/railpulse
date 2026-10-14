package altn72.railpulse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import altn72.railpulse.modele.Programmeur;
import altn72.railpulse.modele.ProgrammeurRepository;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = RailPulseApplication.class)
class RailPulseApplicationTests {

    @Autowired
    private ProgrammeurRepository programmeurRepository;

    @Test
    void contextLoads() {
    }

    @Test
    @Transactional
    void canPersistAndReadProgrammeur() {
        Programmeur programmeur = new Programmeur();
        programmeur.setNom("Martin");
        programmeur.setPrenom("Lea");

        Programmeur saved = programmeurRepository.saveAndFlush(programmeur);

        assertThat(saved.getId()).isNotNull();
        assertThat(programmeurRepository.findById(saved.getId()))
                .get()
                .extracting(Programmeur::getNom)
                .isEqualTo("Martin");
    }

}
