package altn72.railpulse.service.exception;

public class ProgrammeurIntrouvableException extends RuntimeException {
    public ProgrammeurIntrouvableException(Integer id) {
        super("Le programmeur n° " + id + " n'existe pas.");
    }
}
