package altn72.railpulse.service;

import jakarta.validation.constraints.NotNull;

public class AffectationPrestataire {
    @NotNull(message = "Choisissez un prestataire.")
    private Long prestataireId;

    public Long getPrestataireId() {
        return prestataireId;
    }

    public void setPrestataireId(Long prestataireId) {
        this.prestataireId = prestataireId;
    }
}
