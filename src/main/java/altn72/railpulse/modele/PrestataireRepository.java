package altn72.railpulse.modele;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrestataireRepository extends JpaRepository<Prestataire, Long> {
    List<Prestataire> findAllByOrderByNomAscIdAsc();
}
