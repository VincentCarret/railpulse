package altn72.railpulse.config;

import altn72.railpulse.modele.Equipement;
import altn72.railpulse.modele.EquipementRepository;
import altn72.railpulse.modele.EtatFonctionnement;
import altn72.railpulse.modele.Station;
import altn72.railpulse.modele.StationRepository;
import altn72.railpulse.modele.TypeEquipement;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Profile("demo")
public class DonneesDemonstration implements ApplicationRunner {
    private final StationRepository stations;
    private final EquipementRepository equipements;

    public DonneesDemonstration(StationRepository stations, EquipementRepository equipements) {
        this.stations = stations;
        this.equipements = equipements;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        // Ne jamais compléter ni remplacer silencieusement un catalogue déjà renseigné.
        if (stations.count() > 0 || equipements.count() > 0) {
            return;
        }
        Station republique = stations.save(new Station("République"));
        Station centrale = stations.save(new Station("Gare Centrale"));
        stations.save(new Station("Parc des Expositions"));
        equipements.saveAll(List.of(
                new Equipement("REP-ASC-01", "Ascenseur du quai A", TypeEquipement.ASCENSEUR,
                        republique, EtatFonctionnement.EN_SERVICE),
                new Equipement("REP-ESC-01", "Escalier de la sortie nord", TypeEquipement.ESCALIER_MECANIQUE,
                        republique, EtatFonctionnement.EN_PANNE),
                new Equipement("REP-POR-01", "Portique de l'entrée principale", TypeEquipement.PORTIQUE,
                        republique, EtatFonctionnement.EN_MAINTENANCE),
                new Equipement("GAR-DIS-01", "Distributeur du hall", TypeEquipement.DISTRIBUTEUR,
                        centrale, EtatFonctionnement.EN_SERVICE),
                new Equipement("GAR-ECR-01", "Écran du quai B", TypeEquipement.ECRAN_INFORMATION,
                        centrale, EtatFonctionnement.EN_PANNE)));
    }
}
