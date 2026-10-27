package altn72.railpulse.modele;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IncidentRepository extends JpaRepository<Incident, Long> {
    @Query("select i from Incident i left join fetch i.prestataire where i.equipement.id = :equipementId "
            + "order by i.dateOuverture desc, i.id desc")
    List<Incident> rechercherParEquipement(@Param("equipementId") Long equipementId);
}
