package altn72.railpulse.config;

import altn72.railpulse.service.ComptesDemonstrationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Profile("demo")
@Order(1)
public class ComptesDemonstrationAuDemarrage implements ApplicationRunner {
    private final ComptesDemonstrationService comptes;
    private final String motDePasseAgent;
    private final String motDePasseResponsable;

    public ComptesDemonstrationAuDemarrage(ComptesDemonstrationService comptes,
            @Value("${railpulse.demo.agent-password:}") String motDePasseAgent,
            @Value("${railpulse.demo.responsable-password:}") String motDePasseResponsable) {
        this.comptes = comptes;
        this.motDePasseAgent = motDePasseAgent;
        this.motDePasseResponsable = motDePasseResponsable;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        comptes.initialiser(motDePasseAgent, motDePasseResponsable);
    }
}
