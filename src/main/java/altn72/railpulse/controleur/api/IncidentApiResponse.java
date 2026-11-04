package altn72.railpulse.controleur.api;

import altn72.railpulse.modele.Equipement;
import altn72.railpulse.modele.Incident;
import altn72.railpulse.modele.Prestataire;
import altn72.railpulse.modele.StatutIncident;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public record IncidentApiResponse(Long id, String description, StatutIncident statut,
                                  @Schema(format = "date-time", example = "2026-10-27T12:00Z")
                                  String dateOuverture,
                                  @Schema(format = "date-time", nullable = true)
                                  String dateResolution,
                                  EquipementInfo equipement,
                                  @Schema(nullable = true) PrestataireInfo prestataire) {

    public static IncidentApiResponse depuis(Incident incident) {
        Equipement equipement = incident.getEquipement();
        Prestataire prestataire = incident.getPrestataire();
        return new IncidentApiResponse(incident.getId(), incident.getDescription(), incident.getStatut(),
                dateUtc(incident.getDateOuverture()), dateUtc(incident.getDateResolution()),
                new EquipementInfo(equipement.getId(), equipement.getReference(),
                        equipement.getStation().getId(), equipement.getStation().getNom()),
                prestataire == null ? null : new PrestataireInfo(prestataire.getId(), prestataire.getNom()));
    }

    private static String dateUtc(LocalDateTime date) {
        return date == null ? null : date.atOffset(ZoneOffset.UTC).toString();
    }

    public record EquipementInfo(Long id, String reference, Long stationId, String stationNom) {
    }

    public record PrestataireInfo(Long id, String nom) {
    }
}
