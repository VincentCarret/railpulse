package altn72.railpulse.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * L'API réutilise la session du formulaire de connexion plutôt qu'un second mécanisme
 * d'authentification : Swagger UI, servi sur le même domaine, envoie donc le cookie
 * de session automatiquement. Seul le jeton CSRF doit être saisi pour les écritures.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(title = "RailPulse - API des incidents", version = "1.0.0",
                description = """
                        API F12. Connectez-vous d'abord via le formulaire /login ; la session JSESSIONID \
                        est ensuite utilisée pour les appels. Une requête non authentifiée est redirigée \
                        vers /login. Seul le responsable d'exploitation peut résoudre un incident. \
                        Pour une écriture, récupérez le jeton avec GET /api/csrf puis saisissez-le \
                        dans « Authorize » (en-tête X-CSRF-TOKEN). Les dates sont en UTC au format ISO 8601."""),
        security = @SecurityRequirement(name = OpenApiConfig.SESSION))
@SecurityScheme(name = OpenApiConfig.SESSION, type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.COOKIE, paramName = "JSESSIONID")
@SecurityScheme(name = OpenApiConfig.CSRF, type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER, paramName = "X-CSRF-TOKEN")
public class OpenApiConfig {
    public static final String SESSION = "session";
    public static final String CSRF = "csrf";
}
