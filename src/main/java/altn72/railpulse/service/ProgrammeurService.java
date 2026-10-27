package altn72.railpulse.service;

import altn72.railpulse.modele.Programmeur;
import altn72.railpulse.modele.ProgrammeurRepository;
import altn72.railpulse.service.exception.ProgrammeurIntrouvableException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import java.util.List;

@Service
@Validated
@Transactional(readOnly = true)
public class ProgrammeurService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProgrammeurService.class);
    private final ProgrammeurRepository programmeurRepository;

    public ProgrammeurService(ProgrammeurRepository programmeurRepository) {
        this.programmeurRepository = programmeurRepository;
    }

    public List<Programmeur> getProgrammeurs() {
        return programmeurRepository.findAllByOrderByNomAscPrenomAscIdAsc();
    }

    public Programmeur getUnProgrammeur(Integer id) {
        return programmeurRepository.findById(id)
                .orElseThrow(() -> new ProgrammeurIntrouvableException(id));
    }

    @Transactional
    public void supprimerProgrammeur(Integer id) {
        programmeurRepository.delete(getUnProgrammeur(id));
        LOGGER.info("Suppression du programmeur {}", id);
    }

    @Transactional
    public Programmeur ajouterProgrammeur(@Valid Programmeur programmeur) {
        // Un ajout crée toujours une ligne, même si un identifiant est fourni.
        programmeur.setId(null);
        Programmeur created = programmeurRepository.save(programmeur);
        LOGGER.info("Création du programmeur {}", created.getId());
        return created;
    }

    @Transactional
    public Programmeur modifierProgrammeur(Integer id, @Valid Programmeur modifications) {
        Programmeur existing = getUnProgrammeur(id);
        existing.setNom(modifications.getNom());
        existing.setPrenom(modifications.getPrenom());
        existing.setAdresse(modifications.getAdresse());
        existing.setLangageExpertise(modifications.getLangageExpertise());
        existing.setLivrePrefere(modifications.getLivrePrefere());
        existing.setSalaire(modifications.getSalaire());
        // Le dirty checking JPA enregistre l'entité au commit de la transaction.
        LOGGER.info("Modification du programmeur {}", id);
        return existing;
    }
}
