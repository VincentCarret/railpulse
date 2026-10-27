package altn72.railpulse.modele;

public enum EtatFonctionnement {
    EN_SERVICE("En service"),
    EN_PANNE("En panne"),
    EN_MAINTENANCE("En maintenance");

    private final String libelle;

    EtatFonctionnement(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
