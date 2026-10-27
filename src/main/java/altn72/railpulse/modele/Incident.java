package altn72.railpulse.modele;

import altn72.railpulse.service.exception.EtatIncidentInvalideException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Entity
@Table(name = "incident", indexes = @Index(name = "idx_incident_equipement_ouverture",
        columnList = "equipement_id,date_ouverture"))
public class Incident {
    public static final int LONGUEUR_DESCRIPTION_MAX = 2000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "L'équipement de l'incident est obligatoire.")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipement_id", nullable = false)
    private Equipement equipement;

    @NotBlank(message = "La description de l'incident est obligatoire.")
    @Size(max = LONGUEUR_DESCRIPTION_MAX, message = "La description ne doit pas dépasser 2000 caractères.")
    @Column(nullable = false, length = LONGUEUR_DESCRIPTION_MAX)
    private String description;

    @NotNull(message = "La date d'ouverture est obligatoire.")
    @Column(name = "date_ouverture", nullable = false)
    private LocalDateTime dateOuverture;

    @NotNull(message = "Le statut de l'incident est obligatoire.")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutIncident statut;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prestataire_id")
    private Prestataire prestataire;

    @Column(name = "date_resolution")
    private LocalDateTime dateResolution;

    protected Incident() {
    }

    public Incident(Equipement equipement, String description, LocalDateTime dateOuverture) {
        this.equipement = equipement;
        this.description = description;
        this.dateOuverture = dateOuverture;
        this.statut = StatutIncident.NON_AFFECTE;
    }

    public void affecter(Prestataire prestataire) {
        verifierAffectable();
        if (prestataire == null) {
            throw new EtatIncidentInvalideException("Un prestataire est nécessaire pour affecter l'incident.");
        }
        this.prestataire = prestataire;
        this.statut = StatutIncident.AFFECTE;
    }

    public void verifierAffectable() {
        if (statut == StatutIncident.RESOLU) {
            throw new EtatIncidentInvalideException("Un incident résolu ne peut plus être affecté ni réaffecté.");
        }
    }

    public void resoudre(LocalDateTime dateResolution) {
        if (statut == StatutIncident.RESOLU) {
            throw new EtatIncidentInvalideException("Cet incident est déjà résolu.");
        }
        if (dateResolution == null || dateResolution.isBefore(dateOuverture)) {
            throw new EtatIncidentInvalideException("La résolution doit être postérieure ou égale à l'ouverture.");
        }
        this.dateResolution = dateResolution;
        this.statut = StatutIncident.RESOLU;
    }

    public boolean isModifiable() {
        return statut != StatutIncident.RESOLU;
    }

    public void verifierModifiable() {
        if (!isModifiable()) {
            throw new EtatIncidentInvalideException("La description d'un incident résolu ne peut plus être modifiée.");
        }
    }

    public void modifierDescription(String description) {
        verifierModifiable();
        this.description = description.strip();
    }

    public Long getId() {
        return id;
    }

    public Equipement getEquipement() {
        return equipement;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getDateOuverture() {
        return dateOuverture;
    }

    public StatutIncident getStatut() {
        return statut;
    }

    public Prestataire getPrestataire() {
        return prestataire;
    }

    public LocalDateTime getDateResolution() {
        return dateResolution;
    }
}
