package altn72.railpulse.service;

import altn72.railpulse.modele.Incident;

public record ResultatDeclarationIncident(Incident incident, boolean equipementFragile) {
}
