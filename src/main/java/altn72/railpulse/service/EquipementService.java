package altn72.railpulse.service;

import altn72.railpulse.modele.EquipementRepository;
import altn72.railpulse.modele.Equipement;
import altn72.railpulse.modele.IncidentRepository;
import altn72.railpulse.modele.Station;
import altn72.railpulse.modele.StationRepository;
import altn72.railpulse.service.exception.StationIntrouvableException;
import altn72.railpulse.service.exception.EquipementIntrouvableException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class EquipementService {
    private final StationRepository stations;
    private final EquipementRepository equipements;
    private final IncidentRepository incidents;

    public EquipementService(StationRepository stations, EquipementRepository equipements,
                             IncidentRepository incidents) {
        this.stations = stations;
        this.equipements = equipements;
        this.incidents = incidents;
    }

    public List<Station> listerStations() {
        return stations.findAllByOrderByNomAscIdAsc();
    }

    public EquipementsStation consulterStation(Long stationId) {
        Station station = stations.findById(stationId)
                .orElseThrow(() -> new StationIntrouvableException(stationId));
        return new EquipementsStation(station, equipements.rechercherParStation(stationId));
    }

    public Equipement lireEquipement(Long id) {
        return equipements.rechercherAvecStation(id)
                .orElseThrow(() -> new EquipementIntrouvableException(id));
    }

    public FicheEquipement consulterEquipement(Long id) {
        return new FicheEquipement(lireEquipement(id), incidents.rechercherParEquipement(id));
    }
}
