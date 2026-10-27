package altn72.railpulse.service.exception;

public class StationIntrouvableException extends RuntimeException {
    public StationIntrouvableException(Long id) {
        super("La station " + id + " n'existe pas.");
    }
}
