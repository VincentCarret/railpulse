package altn72.railpulse.modele;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IncidentRepository extends JpaRepository<Incident, Long> {
    @Query("select i from Incident i join fetch i.equipement e join fetch e.station "
            + "left join fetch i.prestataire order by i.dateOuverture desc, i.id desc")
    List<Incident> rechercherTousParOuvertureDecroissante();

    @Query("select i from Incident i left join fetch i.prestataire where i.equipement.id = :equipementId "
            + "order by i.dateOuverture desc, i.id desc")
    List<Incident> rechercherParEquipement(@Param("equipementId") Long equipementId);

    @Query("select i from Incident i join fetch i.equipement e join fetch e.station "
            + "left join fetch i.prestataire where i.id = :id")
    Optional<Incident> rechercherDetail(@Param("id") Long id);

    @Query("select i from Incident i join fetch i.equipement e join fetch e.station "
            + "left join fetch i.prestataire where i.statut = :statut "
            + "order by i.dateResolution desc, i.id desc")
    List<Incident> rechercherParStatutEtDateResolutionDecroissante(@Param("statut") StatutIncident statut);

    @Query("select i from Incident i join fetch i.equipement e join fetch e.station "
            + "left join fetch i.prestataire where i.statut <> :statutResolu")
    List<Incident> rechercherActifs(@Param("statutResolu") StatutIncident statutResolu);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Incident i where i.id = :id")
    Optional<Incident> verrouillerPourModification(@Param("id") Long id);
}
