package altn72.railpulse.config;

import altn72.railpulse.modele.Equipement;
import altn72.railpulse.modele.EquipementRepository;
import altn72.railpulse.modele.Incident;
import altn72.railpulse.modele.IncidentRepository;
import altn72.railpulse.modele.Prestataire;
import altn72.railpulse.modele.PrestataireRepository;
import altn72.railpulse.modele.EtatFonctionnement;
import altn72.railpulse.modele.Station;
import altn72.railpulse.modele.StationRepository;
import altn72.railpulse.modele.TypeEquipement;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.Clock;
import java.time.LocalDateTime;

@Component
@Profile("demo")
public class DonneesDemonstration implements ApplicationRunner {
    private final StationRepository stations;
    private final EquipementRepository equipements;
    private final IncidentRepository incidents;
    private final PrestataireRepository prestataires;
    private final Clock horloge;

    public DonneesDemonstration(StationRepository stations, EquipementRepository equipements,
                               IncidentRepository incidents, PrestataireRepository prestataires, Clock horloge) {
        this.stations = stations;
        this.equipements = equipements;
        this.incidents = incidents;
        this.prestataires = prestataires;
        this.horloge = horloge;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        // Ne jamais compléter ni remplacer silencieusement un catalogue déjà renseigné.
        if (stations.count() > 0 || equipements.count() > 0) {
            return;
        }
        Station republique = stations.save(new Station("République"));
        Station centrale = stations.save(new Station("Gare Centrale"));
        stations.save(new Station("Parc des Expositions"));
        Equipement escalier = new Equipement("REP-ESC-01", "Escalier de la sortie nord", TypeEquipement.ESCALIER_MECANIQUE,
                republique, EtatFonctionnement.EN_PANNE);
        Equipement ecran = new Equipement("GAR-ECR-01", "Écran du quai B", TypeEquipement.ECRAN_INFORMATION,
                centrale, EtatFonctionnement.EN_PANNE);
        equipements.saveAll(List.of(
                new Equipement("REP-ASC-01", "Ascenseur du quai A", TypeEquipement.ASCENSEUR,
                        republique, EtatFonctionnement.EN_SERVICE),
                escalier,
                new Equipement("REP-POR-01", "Portique de l'entrée principale", TypeEquipement.PORTIQUE,
                        republique, EtatFonctionnement.EN_MAINTENANCE),
                new Equipement("GAR-DIS-01", "Distributeur du hall", TypeEquipement.DISTRIBUTEUR,
                        centrale, EtatFonctionnement.EN_SERVICE),
                ecran));
        creerIncidents(escalier, ecran);
    }

    private void creerIncidents(Equipement escalier, Equipement ecran) {
        LocalDateTime maintenant = LocalDateTime.now(horloge);
        Prestataire prestataire = prestataires.save(new Prestataire("Maintenance Réseau", "maintenance@example.invalid"));
        Incident affecte = new Incident(escalier, "Bruit anormal en cours de diagnostic.", maintenant.minusHours(96));
        affecte.affecter(prestataire);
        Incident resolu = new Incident(escalier, "Ancien arrêt de l'escalier, intervention terminée.", maintenant.minusHours(168));
        resolu.affecter(prestataire);
        resolu.resoudre(maintenant.minusHours(120));
        incidents.saveAll(List.of(
                new Incident(escalier, "Arrêt intermittent signalé sur l'escalier.", maintenant.minusHours(47)),
                new Incident(ecran, "Affichage figé sur le quai.", maintenant.minusHours(48)),
                new Incident(ecran, "Information voyageurs partiellement illisible.", maintenant.minusHours(72)),
                affecte, resolu));
    }
}
