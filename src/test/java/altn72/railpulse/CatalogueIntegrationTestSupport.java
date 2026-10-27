package altn72.railpulse;

import altn72.railpulse.modele.Equipement;
import altn72.railpulse.modele.EquipementRepository;
import altn72.railpulse.modele.EtatFonctionnement;
import altn72.railpulse.modele.IncidentRepository;
import altn72.railpulse.modele.PrestataireRepository;
import altn72.railpulse.modele.Station;
import altn72.railpulse.modele.StationRepository;
import altn72.railpulse.modele.TypeEquipement;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;

abstract class CatalogueIntegrationTestSupport extends WebIntegrationTestSupport {
    @Autowired
    protected StationRepository stations;

    @Autowired
    protected EquipementRepository equipements;

    @Autowired
    protected IncidentRepository incidents;

    @Autowired
    protected PrestataireRepository prestataires;

    @AfterEach
    void nettoyerCatalogue() {
        incidents.deleteAll();
        prestataires.deleteAll();
        equipements.deleteAll();
        stations.deleteAll();
    }

    protected Equipement creerEquipement(String reference) {
        Station station = stations.save(new Station("République"));
        return equipements.save(new Equipement(reference, "Ascenseur quai A", TypeEquipement.ASCENSEUR,
                station, EtatFonctionnement.EN_SERVICE));
    }
}
