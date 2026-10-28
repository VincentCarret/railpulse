package altn72.railpulse.service;

import altn72.railpulse.modele.StatutIncident;

public enum FiltrePriseEnCharge {
    TOUS("Tous les incidents actifs"),
    NON_AFFECTE("En attente d'affectation"),
    AFFECTE("Pris en charge");

    private final String libelle;

    FiltrePriseEnCharge(String libelle) {
        this.libelle = libelle;
    }

    public boolean inclut(StatutIncident statut) {
        return switch (this) {
            case TOUS -> statut != StatutIncident.RESOLU;
            case NON_AFFECTE -> statut == StatutIncident.NON_AFFECTE;
            case AFFECTE -> statut == StatutIncident.AFFECTE;
        };
    }

    public String getLibelle() {
        return libelle;
    }
}
