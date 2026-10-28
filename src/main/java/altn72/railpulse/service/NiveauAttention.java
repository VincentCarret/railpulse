package altn72.railpulse.service;

import altn72.railpulse.modele.Incident;
import altn72.railpulse.modele.StatutIncident;
import java.time.Duration;
import java.time.LocalDateTime;

public enum NiveauAttention {
    ROUGE("Urgent : 72 h ou plus sans affectation", 0),
    ORANGE("À surveiller : 48 à 72 h sans affectation", 1),
    VERT("Moins de 48 h sans affectation", 2),
    PRIS_EN_CHARGE("Pris en charge", 3);

    public static final Duration SEUIL_ORANGE = Duration.ofHours(48);
    public static final Duration SEUIL_ROUGE = Duration.ofHours(72);

    private final String libelle;
    private final int priorite;

    NiveauAttention(String libelle, int priorite) {
        this.libelle = libelle;
        this.priorite = priorite;
    }

    public static NiveauAttention calculer(Incident incident, LocalDateTime maintenant) {
        if (incident.getStatut() == StatutIncident.AFFECTE) {
            return PRIS_EN_CHARGE;
        }
        Duration ouverture = Duration.between(incident.getDateOuverture(), maintenant);
        if (ouverture.compareTo(SEUIL_ROUGE) >= 0) {
            return ROUGE;
        }
        if (ouverture.compareTo(SEUIL_ORANGE) >= 0) {
            return ORANGE;
        }
        return VERT;
    }

    public String getLibelle() {
        return libelle;
    }

    public int getPriorite() {
        return priorite;
    }
}
