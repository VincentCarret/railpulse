package altn72.railpulse.service;

import altn72.railpulse.modele.Station;
import java.util.List;

public record TableauDeBord(List<IncidentActif> incidents, List<Station> stations,
                            Long stationId, FiltrePriseEnCharge priseEnCharge,
                            long total, long enAttente, long prisEnCharge, long urgents) {
}
