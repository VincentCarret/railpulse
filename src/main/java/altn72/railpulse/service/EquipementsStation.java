package altn72.railpulse.service;

import altn72.railpulse.modele.Equipement;
import altn72.railpulse.modele.Station;

import java.util.List;

public record EquipementsStation(Station station, List<Equipement> equipements) {
}
