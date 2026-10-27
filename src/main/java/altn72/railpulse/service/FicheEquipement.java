package altn72.railpulse.service;

import altn72.railpulse.modele.Equipement;
import altn72.railpulse.modele.Incident;

import java.util.List;

public record FicheEquipement(Equipement equipement, List<Incident> incidents) {
}
