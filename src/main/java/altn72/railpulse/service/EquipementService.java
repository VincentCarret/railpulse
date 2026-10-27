package altn72.railpulse.service;

import altn72.railpulse.modele.EquipementRepository;
import altn72.railpulse.modele.Station;
import altn72.railpulse.modele.StationRepository;
import altn72.railpulse.service.exception.StationIntrouvableException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class EquipementService {
    private final StationRepository stations;
    private final EquipementRepository equipements;

    public EquipementService(StationRepository stations, EquipementRepository equipements) {
        this.stations = stations;
        this.equipements = equipements;
    }

    public List<Station> listerStations() {
        return stations.findAllByOrderByNomAscIdAsc();
    }

    public EquipementsStation consulterStation(Long stationId) {
        Station station = stations.findById(stationId)
                .orElseThrow(() -> new StationIntrouvableException(stationId));
        return new EquipementsStation(station, equipements.rechercherParStation(stationId));
    }
}
