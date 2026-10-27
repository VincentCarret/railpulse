package altn72.railpulse.service.exception;

public class PrestataireIntrouvableException extends RuntimeException {
    public PrestataireIntrouvableException(Long id) {
        super("Le prestataire n° " + id + " n'existe pas.");
    }
}
