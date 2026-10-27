package altn72.railpulse.modele;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EquipementRepository extends JpaRepository<Equipement, Long> {
    @Query("select e from Equipement e where e.station.id = :stationId order by e.reference asc")
    List<Equipement> rechercherParStation(@Param("stationId") Long stationId);

    @Query("select e from Equipement e join fetch e.station where e.id = :id")
    Optional<Equipement> rechercherAvecStation(@Param("id") Long id);

    @Query(value = "select e.reference as reference, e.libelle as libelle, e.type as type, "
            + "e.etat_fonctionnement as etat, s.nom as station "
            + "from {h-schema}equipement e join {h-schema}station s on s.id = e.station_id "
            + "order by s.nom, e.reference", nativeQuery = true)
    List<LigneCatalogue> exporterCatalogue();
}
