package altn72.railpulse.service;

import altn72.railpulse.modele.IncidentRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FragiliteEquipementService {
    public static final int SEUIL_INCIDENTS = 3;
    public static final int FENETRE_JOURS = 30;

    private final IncidentRepository incidents;
    private final Clock horloge;

    public FragiliteEquipementService(IncidentRepository incidents, Clock horloge) {
        this.incidents = incidents;
        this.horloge = horloge;
    }

    public boolean estFragile(Long equipementId) {
        return rechercherFragiles(Set.of(equipementId), LocalDateTime.now(horloge)).contains(equipementId);
    }

    public Set<Long> rechercherFragiles(Collection<Long> equipementIds, LocalDateTime maintenant) {
        if (equipementIds.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(incidents.rechercherEquipementsFragiles(equipementIds,
                maintenant.minusDays(FENETRE_JOURS), SEUIL_INCIDENTS));
    }
}
