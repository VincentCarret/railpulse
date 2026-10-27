package altn72.railpulse.service;

import altn72.railpulse.modele.EquipementRepository;
import altn72.railpulse.modele.EtatFonctionnement;
import altn72.railpulse.modele.LigneCatalogue;
import altn72.railpulse.modele.TypeEquipement;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExportCatalogueService {
    private final EquipementRepository equipements;

    public ExportCatalogueService(EquipementRepository equipements) {
        this.equipements = equipements;
    }

    @Transactional(readOnly = true)
    public String exporter() {
        StringBuilder csv = new StringBuilder("\uFEFFRéférence;Libellé;Type;État;Station\r\n");
        for (LigneCatalogue ligne : equipements.exporterCatalogue()) {
            csv.append(List.of(ligne.getReference(), ligne.getLibelle(),
                            TypeEquipement.valueOf(ligne.getType()).getLibelle(),
                            EtatFonctionnement.valueOf(ligne.getEtat()).getLibelle(), ligne.getStation())
                    .stream().map(this::cellule).collect(Collectors.joining(";"))).append("\r\n");
        }
        return csv.toString();
    }

    private String cellule(String valeur) {
        // Un tableur ne doit pas interpréter les données métier comme des formules.
        String debut = valeur.stripLeading();
        if (!debut.isEmpty() && "=+-@".indexOf(debut.charAt(0)) >= 0) {
            valeur = "'" + valeur;
        }
        return "\"" + valeur.replace("\"", "\"\"") + "\"";
    }
}
