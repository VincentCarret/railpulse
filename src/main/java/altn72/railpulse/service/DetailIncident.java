package altn72.railpulse.service;

import altn72.railpulse.modele.Evaluation;
import altn72.railpulse.modele.Incident;

public record DetailIncident(Incident incident, Evaluation evaluation, String dureeOuverture) {
}
