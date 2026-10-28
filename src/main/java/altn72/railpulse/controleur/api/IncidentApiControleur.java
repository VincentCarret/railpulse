package altn72.railpulse.controleur.api;

import altn72.railpulse.service.IncidentService;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class IncidentApiControleur {
    private final IncidentService incidents;

    public IncidentApiControleur(IncidentService incidents) {
        this.incidents = incidents;
    }

    @GetMapping("/csrf")
    public ResponseEntity<JetonCsrf> csrf(CsrfToken jeton) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new JetonCsrf(jeton.getHeaderName(), jeton.getToken()));
    }

    @GetMapping("/incidents")
    public ResponseEntity<List<IncidentApiResponse>> lister() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(incidents.consulterTous().stream().map(IncidentApiResponse::depuis).toList());
    }

    @GetMapping("/incidents/{id}")
    public ResponseEntity<IncidentApiResponse> consulter(@PathVariable Long id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(IncidentApiResponse.depuis(incidents.lireIncident(id)));
    }

    @PostMapping("/incidents/{id}/resolution")
    public ResponseEntity<IncidentApiResponse> resoudre(@PathVariable Long id) {
        incidents.resoudreIncident(id);
        return consulter(id);
    }

    public record JetonCsrf(String headerName, String token) {
    }
}
