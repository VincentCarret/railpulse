package altn72.railpulse.service.exception;

public class IncidentIntrouvableException extends RuntimeException {
    public IncidentIntrouvableException(Long id) {
        super("L'incident " + id + " n'existe pas.");
    }
}
