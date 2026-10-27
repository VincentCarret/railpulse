package altn72.railpulse.modele;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProgrammeurRepository extends JpaRepository<Programmeur, Integer> {
    List<Programmeur> findAllByOrderByNomAscPrenomAscIdAsc();
}
