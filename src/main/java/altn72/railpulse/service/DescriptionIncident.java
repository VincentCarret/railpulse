package altn72.railpulse.service;

import altn72.railpulse.modele.Incident;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class DescriptionIncident {
    @NotBlank(message = "Décrivez le problème rencontré sur cet équipement.")
    @Size(max = Incident.LONGUEUR_DESCRIPTION_MAX, message = "La description ne doit pas dépasser 2000 caractères.")
    private String description;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
