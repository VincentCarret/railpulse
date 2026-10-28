package altn72.railpulse.modele;

import altn72.railpulse.service.exception.EtatIncidentInvalideException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Entity
@Table(name = "evaluation")
public class Evaluation {
    public static final int NOTE_MIN = 1;
    public static final int NOTE_MAX = 5;
    public static final int LONGUEUR_COMMENTAIRE_MAX = 2000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "L'incident évalué est obligatoire.")
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_id", nullable = false, unique = true)
    private Incident incident;

    @Min(value = NOTE_MIN, message = "La note doit être comprise entre 1 et 5.")
    @Max(value = NOTE_MAX, message = "La note doit être comprise entre 1 et 5.")
    @Column(nullable = false)
    private int note;

    @Size(max = LONGUEUR_COMMENTAIRE_MAX, message = "Le commentaire ne doit pas dépasser 2000 caractères.")
    @Column(length = LONGUEUR_COMMENTAIRE_MAX)
    private String commentaire;

    @NotNull(message = "La date de l'évaluation est obligatoire.")
    @Column(name = "date_evaluation", nullable = false)
    private LocalDateTime date;

    protected Evaluation() {
    }

    public Evaluation(Incident incident, int note, String commentaire, LocalDateTime date) {
        if (incident == null) {
            throw new EtatIncidentInvalideException("Seul un incident résolu avec un prestataire peut être évalué.");
        }
        incident.verifierEvaluable();
        if (note < NOTE_MIN || note > NOTE_MAX) {
            throw new EtatIncidentInvalideException("La note doit être comprise entre 1 et 5.");
        }
        if (date == null || date.isBefore(incident.getDateResolution())) {
            throw new EtatIncidentInvalideException("La date d'évaluation doit être postérieure ou égale à la résolution.");
        }
        this.incident = incident;
        this.note = note;
        this.commentaire = commentaire == null ? null : commentaire.strip();
        this.date = date;
    }

    public Long getId() {
        return id;
    }

    public Incident getIncident() {
        return incident;
    }

    public int getNote() {
        return note;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public LocalDateTime getDate() {
        return date;
    }
}
