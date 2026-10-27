package altn72.railpulse.controleur;

import altn72.railpulse.service.exception.ProgrammeurIntrouvableException;
import altn72.railpulse.service.exception.StationIntrouvableException;
import altn72.railpulse.service.exception.EquipementIntrouvableException;
import altn72.railpulse.service.exception.EtatIncidentInvalideException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final GlobalUserControllerAdvice userContext;

    public GlobalExceptionHandler(GlobalUserControllerAdvice userContext) {
        this.userContext = userContext;
    }

    @ExceptionHandler(ProgrammeurIntrouvableException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String introuvable(ProgrammeurIntrouvableException exception, Model model) {
        return erreur(model, 404, exception.getMessage());
    }

    @ExceptionHandler(StationIntrouvableException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String stationIntrouvable(StationIntrouvableException exception, Model model) {
        return erreur(model, 404, exception.getMessage());
    }

    @ExceptionHandler(EquipementIntrouvableException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String equipementIntrouvable(EquipementIntrouvableException exception, Model model) {
        return erreur(model, 404, exception.getMessage());
    }

    @ExceptionHandler(EtatIncidentInvalideException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String etatIncidentInvalide(EtatIncidentInvalideException exception, Model model) {
        return erreur(model, 409, exception.getMessage());
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, ConstraintViolationException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String saisieInvalide(Model model) {
        return erreur(model, 400, "Les informations transmises ne sont pas valides.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String conflit(Model model) {
        LOGGER.warn("Une opération a été refusée par les contraintes de la base.");
        return erreur(model, 409, "L'opération ne peut pas être enregistrée. Vérifiez les informations ou rechargez la page.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String accesRefuse(Model model) {
        return erreur(model, 403, "Vous n'avez pas accès à cette opération.");
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView inattendue(Exception exception, Model model) {
        if (exception instanceof ErrorResponse error) {
            int status = error.getStatusCode().value();
            String message = switch (status) {
                case 404 -> "La page demandée n'existe pas.";
                case 405 -> "Cette action n'est pas disponible à cette adresse.";
                default -> "La demande n'a pas pu aboutir.";
            };
            erreur(model, status, message);
            return new ModelAndView("error", model.asMap(), error.getStatusCode());
        }
        LOGGER.error("Erreur inattendue pendant le traitement d'une requête", exception);
        erreur(model, 500, "Une erreur est survenue. Réessayez plus tard.");
        return new ModelAndView("error", model.asMap(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private String erreur(Model model, int status, String message) {
        // Spring MVC crée un nouveau Model pour les vues issues d'un ExceptionHandler.
        userContext.ajouterUtilisateur(model, SecurityContextHolder.getContext().getAuthentication());
        model.addAttribute("status", status);
        model.addAttribute("messageUtilisateur", message);
        return "error";
    }
}
