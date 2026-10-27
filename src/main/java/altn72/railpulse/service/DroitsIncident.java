package altn72.railpulse.service;

import altn72.railpulse.modele.Incident;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class DroitsIncident {
    private static final String ROLE_RESPONSABLE = "ROLE_RESPONSABLE_EXPLOITATION";

    private boolean estResponsable(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                .anyMatch(autorite -> ROLE_RESPONSABLE.equals(autorite.getAuthority()));
    }

    public boolean peutModifier(Incident incident, Authentication authentication) {
        return estResponsable(authentication) && incident.isModifiable();
    }

    public boolean peutAffecter(Incident incident, Authentication authentication) {
        return estResponsable(authentication) && incident.isModifiable();
    }

    public boolean peutResoudre(Incident incident, Authentication authentication) {
        return estResponsable(authentication) && incident.isModifiable();
    }
}
