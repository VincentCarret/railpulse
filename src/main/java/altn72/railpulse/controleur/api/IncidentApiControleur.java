package altn72.railpulse.controleur.api;

import altn72.railpulse.config.OpenApiConfig;
import altn72.railpulse.controleur.api.IncidentApiErreurs.Erreur;
import altn72.railpulse.service.IncidentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Incidents", description = "Consultation et résolution des incidents (F12)")
@ApiResponse(responseCode = "302", description = "Redirection vers /login si la session est absente")
@RestController
@RequestMapping("/api")
public class IncidentApiControleur {
    private static final String IDENTIFIANT_INCIDENT = "Identifiant de l'incident";

    private final IncidentService incidents;

    public IncidentApiControleur(IncidentService incidents) {
        this.incidents = incidents;
    }

    @Operation(summary = "Obtenir le jeton CSRF de la session",
            description = "Renvoyer le jeton dans l'en-tête indiqué pour toute écriture.")
    @ApiResponse(responseCode = "200", description = "Jeton de la session")
    @GetMapping("/csrf")
    public ResponseEntity<JetonCsrf> csrf(@Parameter(hidden = true) CsrfToken jeton) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new JetonCsrf(jeton.getHeaderName(), jeton.getToken()));
    }

    @Operation(summary = "Lister les incidents",
            description = "Tous les incidents, triés par ouverture décroissante. Accessible aux deux profils.")
    @ApiResponse(responseCode = "200", description = "Liste, éventuellement vide")
    @GetMapping("/incidents")
    public ResponseEntity<List<IncidentApiResponse>> lister() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(incidents.consulterTous().stream().map(IncidentApiResponse::depuis).toList());
    }

    @Operation(summary = "Consulter un incident", description = "Accessible aux deux profils.")
    @ApiResponse(responseCode = "200", description = "Incident trouvé")
    @ApiResponse(responseCode = "400", description = "Identifiant non numérique",
            content = @Content(schema = @Schema(implementation = Erreur.class)))
    @ApiResponse(responseCode = "404", description = "Aucun incident avec cet identifiant",
            content = @Content(schema = @Schema(implementation = Erreur.class)))
    @GetMapping("/incidents/{id}")
    public ResponseEntity<IncidentApiResponse> consulter(
            @Parameter(description = IDENTIFIANT_INCIDENT) @PathVariable Long id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(IncidentApiResponse.depuis(incidents.lireIncident(id)));
    }

    @Operation(summary = "Résoudre un incident actif",
            description = """
                    Réservé au responsable d'exploitation. La date de résolution est générée par le serveur ; \
                    aucun corps de requête n'est accepté pour la fixer. Un incident déjà résolu ou ouvert \
                    dans le futur renvoie 409 sans modification.""",
            security = {@SecurityRequirement(name = OpenApiConfig.SESSION),
                    @SecurityRequirement(name = OpenApiConfig.CSRF)})
    @ApiResponse(responseCode = "200", description = "Incident résolu, avec son nouvel état")
    @ApiResponse(responseCode = "400", description = "Identifiant non numérique",
            content = @Content(schema = @Schema(implementation = Erreur.class)))
    @ApiResponse(responseCode = "403", description = "Profil insuffisant ou jeton CSRF absent/invalide",
            content = @Content(schema = @Schema(implementation = Erreur.class)))
    @ApiResponse(responseCode = "404", description = "Aucun incident avec cet identifiant",
            content = @Content(schema = @Schema(implementation = Erreur.class)))
    @ApiResponse(responseCode = "409", description = "Transition d'état impossible",
            content = @Content(schema = @Schema(implementation = Erreur.class)))
    @PostMapping("/incidents/{id}/resolution")
    public ResponseEntity<IncidentApiResponse> resoudre(
            @Parameter(description = IDENTIFIANT_INCIDENT) @PathVariable Long id) {
        incidents.resoudreIncident(id);
        return consulter(id);
    }

    public record JetonCsrf(@Schema(example = "X-CSRF-TOKEN") String headerName, String token) {
    }
}
