package altn72.railpulse.service;

import altn72.railpulse.modele.Evaluation;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class NouvelleEvaluation {
    @NotNull(message = "Choisissez une note.")
    @Min(value = Evaluation.NOTE_MIN, message = "La note doit être comprise entre 1 et 5.")
    @Max(value = Evaluation.NOTE_MAX, message = "La note doit être comprise entre 1 et 5.")
    private Integer note;

    @Size(max = Evaluation.LONGUEUR_COMMENTAIRE_MAX,
            message = "Le commentaire ne doit pas dépasser 2000 caractères.")
    private String commentaire;

    public Integer getNote() {
        return note;
    }

    public void setNote(Integer note) {
        this.note = note;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public void setCommentaire(String commentaire) {
        this.commentaire = commentaire;
    }
}
