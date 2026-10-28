package altn72.railpulse.service;

import altn72.railpulse.modele.Incident;

public record IncidentActif(Incident incident, NiveauAttention attention, String dureeOuverture) {
}
