package altn72.railpulse.modele;

import java.util.regex.Pattern;

public final class FormatMotDePasse {
    public static final String BCRYPT = "(?:\\{bcrypt\\})?\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}";
    private static final Pattern FORMAT = Pattern.compile(BCRYPT);

    private FormatMotDePasse() {
    }

    public static boolean estBcrypt(String valeur) {
        return valeur != null && FORMAT.matcher(valeur).matches();
    }

    public static String normaliser(String valeur) {
        if (!estBcrypt(valeur)) {
            throw new IllegalArgumentException("Un mot de passe haché avec BCrypt est obligatoire.");
        }
        return valeur.startsWith("{bcrypt}") ? valeur : "{bcrypt}" + valeur;
    }
}
