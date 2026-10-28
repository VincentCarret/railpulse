package altn72.railpulse.controleur.api;

import altn72.railpulse.service.exception.EtatIncidentInvalideException;
import altn72.railpulse.service.exception.IncidentIntrouvableException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Order(0)
@RestControllerAdvice(assignableTypes = IncidentApiControleur.class)
public class IncidentApiErreurs {
    @ExceptionHandler(IncidentIntrouvableException.class)
    public ResponseEntity<Erreur> introuvable(IncidentIntrouvableException exception) {
        return erreur(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(EtatIncidentInvalideException.class)
    public ResponseEntity<Erreur> conflit(EtatIncidentInvalideException exception) {
        return erreur(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Erreur> identifiantInvalide() {
        return erreur(HttpStatus.BAD_REQUEST, "L'identifiant d'incident doit être un entier.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Erreur> interdit() {
        return erreur(HttpStatus.FORBIDDEN, "Vous n'avez pas accès à cette opération.");
    }

    private ResponseEntity<Erreur> erreur(HttpStatus statut, String message) {
        return ResponseEntity.status(statut).body(new Erreur(statut.value(), message));
    }

    public record Erreur(int code, String message) {
    }
}
