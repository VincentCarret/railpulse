package altn72.railpulse.modele;

public enum StatutIncident {
    NON_AFFECTE("Non affecté"),
    AFFECTE("Affecté"),
    RESOLU("Résolu");

    private final String libelle;

    StatutIncident(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
