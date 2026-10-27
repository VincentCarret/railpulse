package altn72.railpulse.service.exception;

public class EquipementIntrouvableException extends RuntimeException {
    public EquipementIntrouvableException(Long id) {
        super("L'équipement " + id + " n'existe pas.");
    }
}
