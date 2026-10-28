package altn72.railpulse.service;

import altn72.railpulse.modele.Incident;
import altn72.railpulse.modele.IncidentRepository;
import altn72.railpulse.modele.StationRepository;
import altn72.railpulse.modele.StatutIncident;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TableauDeBordService {
    private final IncidentRepository incidents;
    private final StationRepository stations;
    private final Clock horloge;

    public TableauDeBordService(IncidentRepository incidents, StationRepository stations, Clock horloge) {
        this.incidents = incidents;
        this.stations = stations;
        this.horloge = horloge;
    }

    @PreAuthorize("hasRole('RESPONSABLE_EXPLOITATION')")
    public TableauDeBord consulter(Long stationId, FiltrePriseEnCharge priseEnCharge) {
        LocalDateTime maintenant = LocalDateTime.now(horloge);
        List<IncidentActif> visibles = incidents.rechercherActifs(StatutIncident.RESOLU).stream()
                .filter(incident -> stationId == null || Objects.equals(incident.getEquipement().getStation().getId(), stationId))
                .filter(incident -> priseEnCharge.inclut(incident.getStatut()))
                .map(incident -> new IncidentActif(incident, NiveauAttention.calculer(incident, maintenant),
                        formaterDuree(incident, maintenant)))
                .sorted(Comparator.comparingInt((IncidentActif ligne) -> ligne.attention().getPriorite())
                        .thenComparing(ligne -> ligne.incident().getDateOuverture())
                        .thenComparing(ligne -> ligne.incident().getId(), Comparator.reverseOrder()))
                .toList();
        long enAttente = visibles.stream().filter(ligne -> ligne.incident().getStatut() == StatutIncident.NON_AFFECTE).count();
        long urgents = visibles.stream().filter(ligne -> ligne.attention() == NiveauAttention.ROUGE).count();
        return new TableauDeBord(visibles, stations.findAllByOrderByNomAscIdAsc(), stationId, priseEnCharge,
                visibles.size(), enAttente, visibles.size() - enAttente, urgents);
    }

    private String formaterDuree(Incident incident, LocalDateTime maintenant) {
        Duration duree = Duration.between(incident.getDateOuverture(), maintenant);
        if (duree.isNegative()) {
            duree = Duration.ZERO;
        }
        return "%d j %d h %d min".formatted(duree.toDays(), duree.toHoursPart(), duree.toMinutesPart());
    }
}
